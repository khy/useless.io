package services.budget

import java.util.{Date, UUID}
import java.sql
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.{LocalDate, DateTime}
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget._
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util._

object PlannedTransactionsService {

  def default()(implicit app: Application) = {
    new PlannedTransactionsService(
      transactionService = TransactionsService.default(),
      usersHelper = UsersHelper.default()
    )
  }

}

class PlannedTransactionsService(
  transactionService: TransactionsService,
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(
    records: Seq[PlannedTransactionRecord]
  )(implicit ec: ExecutionContext): Future[Seq[PlannedTransaction]] = {
    val userGuids = records.map(_.createdByAccount)
    val futUsers = usersHelper.getUsers(userGuids)

    val transactionTypesQuery = TransactionTypes.filter { _.id inSet records.map(_.transactionTypeId) }
    val futTransactionTypes = database.run(transactionTypesQuery.result)

    val accountQuery = Accounts.filter { _.id inSet records.map(_.accountId) }
    val futAccounts = database.run(accountQuery.result)

    val transactionsQuery = Transactions.filter { _.plannedTransactionId inSet records.map(_.id) }
    val futTransactions = database.run(transactionsQuery.result).flatMap { records =>
      transactionService.records2models(records).map { models =>
        models.map { model =>
          val record = records.find(_.guid == model.guid).getOrElse {
            throw new ResourceUnexpectedlyNotFound("Transaction", model.guid)
          }

          (record.plannedTransactionId, model)
        }
      }
    }

    for {
      users <- futUsers
      transactionTypes <- futTransactionTypes
      accounts <- futAccounts
      transactions <- futTransactions
    } yield {
      records.map { record =>
        PlannedTransaction(
          guid = record.guid,
          transactionTypeGuid = transactionTypes.find(_.id == record.transactionTypeId).map(_.guid).getOrElse {
            throw new ResourceUnexpectedlyNotFound("TransactionType", record.transactionTypeId)
          },
          accountGuid = accounts.find(_.id == record.accountId).map(_.guid).getOrElse {
            throw new ResourceUnexpectedlyNotFound("Account", record.accountId)
          },
          minAmount = record.minAmount,
          maxAmount = record.maxAmount,
          minDate = record.minDate.map { new LocalDate(_) },
          maxDate = record.maxDate.map { new LocalDate(_) },
          name = record.name,
          transactions = transactions.filter { case (plannedTransactionId, _) =>
            plannedTransactionId == Some(record.id)
          }.map { case (_, transaction) => transaction },
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findPlannedTransactions(
    ids: Option[Seq[Long]] = None,
    guids: Option[Seq[UUID]] = None,
    contextGuids: Option[Seq[UUID]] = None,
    accountGuids: Option[Seq[UUID]] = None,
    transactionTypeGuids: Option[Seq[UUID]] = None,
    minDateFrom: Option[LocalDate] = None,
    minDateTo: Option[LocalDate] = None,
    maxDateFrom: Option[LocalDate] = None,
    maxDateTo: Option[LocalDate] = None,
    transactionCountFrom: Option[Int] = None,
    transactionCountTo: Option[Int] = None,
    userGuids: Option[Seq[UUID]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[PlannedTransaction]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var query = PlannedTransactions.join(Accounts).on { case (plannedTxn, account) =>
        plannedTxn.accountId === account.id
      }.filter { case (plannedTxn, _) =>
        plannedTxn.deletedAt.isEmpty
      }

      ids.foreach { ids =>
        query = query.filter { case (plannedTxn, _) =>
          plannedTxn.id inSet ids
        }
      }

      guids.foreach { guids =>
        query = query.filter { case (plannedTxn, _) =>
          plannedTxn.guid inSet guids
        }
      }

      accountGuids.foreach { accountGuids =>
        query = query.filter { case (_, account) =>
          account.guid inSet accountGuids
        }
      }

      contextGuids.foreach { contextGuids =>
        val subQuery = Accounts.join(Contexts).on { case (account, context) =>
          account.contextId === context.id && context.deletedAt.isEmpty
        }.filter { case (_, context) =>
          context.guid inSet contextGuids
        }.map { case (account, _) =>
          account.id
        }

        query = query.filter { case (plannedTxn, _) =>
          plannedTxn.accountId in subQuery
        }
      }

      transactionTypeGuids.foreach { transactionTypeGuids =>
        val transactionTypeIdsSubQuery = TransactionTypes.
          filter(_.guid inSet transactionTypeGuids).
          map(_.id)

        query = query.filter { case (plannedTransaction, _) =>
          plannedTransaction.transactionTypeId in transactionTypeIdsSubQuery
        }
      }

      minDateFrom.foreach { minDateFrom =>
        query = query.filter { case (plannedTransaction, _) =>
          plannedTransaction.minDate >= new sql.Date(minDateFrom.toDateTimeAtStartOfDay.getMillis)
        }
      }

      minDateTo.foreach { minDateTo =>
        query = query.filter { case (plannedTransaction, _) =>
          plannedTransaction.minDate <= new sql.Date(minDateTo.toDateTimeAtStartOfDay.getMillis)
        }
      }

      maxDateFrom.foreach { maxDateFrom =>
        query = query.filter { case (plannedTransaction, _) =>
          plannedTransaction.maxDate >= new sql.Date(maxDateFrom.toDateTimeAtStartOfDay.getMillis)
        }
      }

      maxDateTo.foreach { maxDateTo =>
        query = query.filter { case (plannedTransaction, _) =>
          plannedTransaction.maxDate <= new sql.Date(maxDateTo.toDateTimeAtStartOfDay.getMillis)
        }
      }

      transactionCountFrom.foreach { transactionCountFrom =>
        query = query.filter { case (plannedTransaction, _) =>
          Transactions.
            filter(_.plannedTransactionId === plannedTransaction.id).
            length >= transactionCountFrom
        }
      }

      transactionCountTo.foreach { transactionCountTo =>
        query = query.filter { case (plannedTransaction, _) =>
          Transactions.
            filter(_.plannedTransactionId === plannedTransaction.id).
            length <= transactionCountTo
        }
      }

      userGuids.foreach { userGuids =>
        val subQuery = Accounts.join(ContextUsers).on { case (account, contextUser) =>
          account.contextId === contextUser.contextId && contextUser.deletedAt.isEmpty
        }.filter { case (_, contextUser) =>
          contextUser.userGuid inSet userGuids
        }.map { case (account, _) =>
          account.id
        }

        query = query.filter { case (plannedTxn, _) =>
          plannedTxn.accountId in subQuery
        }
      }

      var pageQuery = query.
        sortBy { case (plannedTxn, _) => plannedTxn.createdAt.desc }.
        sortBy { case (plannedTxn, _) => plannedTxn.minDate.desc }

      pageQuery = paginationParams match {
        case params: OffsetBasedPaginationParams => {
          pageQuery.drop(params.offset)
        }
        case _ => pageQuery
      }

      pageQuery = pageQuery.take(paginationParams.limit)

      for {
        total <- database.run(query.length.result)
        results <- database.run(pageQuery.result)
        plannedTxnRecords = results.map { case (plannedTxnRecord, _) => plannedTxnRecord }
        models <- records2models(plannedTxnRecords)
      } yield {
        PaginatedResult.build(models, paginationParams, Some(total))
      }
    }
  }

  def createPlannedTransaction(
    transactionTypeGuid: UUID,
    accountGuid: UUID,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minDate: Option[LocalDate],
    maxDate: Option[LocalDate],
    name: Option[String],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[PlannedTransaction]] = {
    val transactionTypesQuery = TransactionTypes.filter { _.guid === transactionTypeGuid }
    val futValTransactionTypeId = database.run(transactionTypesQuery.result).map { transactionTypes =>
      transactionTypes.headOption.map { transactionType =>
        Validation.success(transactionType.id)
      }.getOrElse {
        Validation.failure("transactionTypeGuid", "useless.error.unknownGuid", "specified" -> transactionTypeGuid.toString)
      }
    }

    val accountQuery = Accounts.filter { _.guid === accountGuid }
    val futValAccountId = database.run(accountQuery.result).map { accounts =>
      accounts.headOption.map { account =>
        Validation.success(account.id)
      }.getOrElse {
        Validation.failure("accountGuid", "useless.error.unknownGuid", "specified" -> accountGuid.toString)
      }
    }

    futValTransactionTypeId.flatMap { valTransactionTypeId =>
      futValAccountId.flatMap { valAccountId =>
        ValidationUtil.mapFuture(valTransactionTypeId ++ valAccountId) { case (transactionTypeId, accountId) =>
          val plannedTransactions = PlannedTransactions.map { r =>
            (r.guid, r.transactionTypeId, r.accountId, r.minAmount, r.maxAmount, r.minDate, r.maxDate, r.name, r.createdByAccount, r.createdByAccessToken)
          }.returning(PlannedTransactions.map(_.id))

          val insert = plannedTransactions += (
            UUID.randomUUID,
            transactionTypeId,
            accountId,
            minAmount,
            maxAmount,
            minDate.map { d => new sql.Date(d.toDate.getTime) },
            maxDate.map { d => new sql.Date(d.toDate.getTime) },
            name,
            accessToken.resourceOwner.guid,
            accessToken.guid
          )

          database.run(insert).flatMap { id =>
            findPlannedTransactions(ids = Some(Seq(id))).map { result =>
              result.map(_.items.headOption) match {
                case Validation.Success(Some(plannedTransaction)) => plannedTransaction
                case _ => throw new ResourceUnexpectedlyNotFound("PlannedTransaction", id)
              }
            }
          }
        }
      }
    }
  }

  def deletePlannedTransaction(
    plannedTransactionGuid: UUID,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[Boolean]] = {
    val query = PlannedTransactions.filter { r => r.guid === plannedTransactionGuid }
    database.run(query.result).flatMap { records =>
      records.headOption.map { record =>
        if (record.createdByAccount != accessToken.resourceOwner.guid) {
          Future.successful {
            Validation.failure("plannedTransactionGuid", "useless.error.unauthorized", "specified" -> plannedTransactionGuid.toString)
          }
        } else {
          val now = new sql.Timestamp((new Date).getTime)

          val query = PlannedTransactions.filter { plannedTransaction =>
            plannedTransaction.id === record.id &&
            plannedTransaction.deletedAt.isEmpty
          }.map { plannedTransaction =>
            (plannedTransaction.deletedAt, plannedTransaction.deletedByAccount, plannedTransaction.deletedByAccessToken)
          }.update((Some(now), Some(accessToken.resourceOwner.guid), Some(accessToken.guid)))

          database.run(query).map { result =>
            Validation.success(result > 0)
          }
        }
      }.getOrElse {
        Future.successful {
          Validation.failure("plannedTransactionGuid", "useless.error.unknownGuid", "specified" -> plannedTransactionGuid.toString)
        }
      }
    }
  }

}
