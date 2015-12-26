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

  def default()(implicit app: Application) = new PlannedTransactionsService(UsersHelper.default())

}

class PlannedTransactionsService(
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
    val futTransactions = database.run(transactionsQuery.result)

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
          transactionGuid = transactions.find(_.plannedTransactionId == Some(record.id)).map(_.guid),
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findPlannedTransactions(
    ids: Option[Seq[Long]] = None,
    createdByAccounts: Option[Seq[UUID]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[PlannedTransaction]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.future(valPaginationParams) { paginationParams =>
      var query = PlannedTransactions.filter(_.deletedAt.isEmpty)

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
      }

      createdByAccounts.foreach { createdByAccounts =>
        query = query.filter { _.createdByAccount inSet createdByAccounts }
      }

      query = query.sortBy(_.minDate.asc)

      database.run(query.result).flatMap { records =>
        records2models(records).map { plannedTransactions =>
          PaginatedResult.build(plannedTransactions, paginationParams, None)
        }
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
        ValidationUtil.future(valTransactionTypeId ++ valAccountId) { case (transactionTypeId, accountId) =>
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
