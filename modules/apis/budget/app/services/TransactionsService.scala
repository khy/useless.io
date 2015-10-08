package services.budget

import java.util.{Date, UUID}
import java.sql.Timestamp
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget._
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util._

object TransactionsService {

  def default()(implicit app: Application) = new TransactionsService(UsersHelper.default())

}

class TransactionsService(
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(records: Seq[TransactionRecord])(implicit ec: ExecutionContext): Future[Seq[Transaction]] = {
    val userGuids = records.map(_.createdByAccount)
    val futUsers = usersHelper.getUsers(userGuids)

    val transactionTypesQuery = TransactionTypes.filter { _.id inSet records.map(_.transactionTypeId) }
    val futTransactionTypes = database.run(transactionTypesQuery.result)

    val accountQuery = Accounts.filter { _.id inSet records.map(_.accountId) }
    val futAccounts = database.run(accountQuery.result)

    val transactionIds = records.flatMap { record =>
      record.plannedTransactionId.toSeq ++ record.adjustedTransactionId.toSeq
    }
    val transactionsQuery = Transactions.filter { _.id inSet transactionIds }
    val futTransactions = database.run(transactionsQuery.result)

    for {
      users <- futUsers
      transactionTypes <- futTransactionTypes
      accounts <- futAccounts
      transactions <- futTransactions
    } yield {
      records.map { record =>
        Transaction(
          guid = record.guid,
          transactionTypeGuid = transactionTypes.find(_.id == record.transactionTypeId).map(_.guid).getOrElse {
            throw new ResourceUnexpectedlyNotFound("TransactionType", record.transactionTypeId)
          },
          accountGuid = accounts.find(_.id == record.accountId).map(_.guid).getOrElse {
            throw new ResourceUnexpectedlyNotFound("Account", record.accountId)
          },
          amount = record.amount,
          timestamp = new DateTime(record.timestamp),
          plannedTransactionGuid = record.plannedTransactionId.map { plannedTransactionId =>
            transactions.find(_.id == plannedTransactionId).map(_.guid).getOrElse {
              throw new ResourceUnexpectedlyNotFound("Transaction", plannedTransactionId)
            }
          },
          adjustedTransactionGuid = record.adjustedTransactionId.map { adjustedTransactionId =>
            transactions.find(_.id == adjustedTransactionId).map(_.guid).getOrElse {
              throw new ResourceUnexpectedlyNotFound("Transaction", adjustedTransactionId)
            }
          },
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findTransactions(
    ids: Option[Seq[Long]] = None,
    guids: Option[Seq[UUID]] = None,
    createdByAccounts: Option[Seq[UUID]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[Transaction]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.future(valPaginationParams) { paginationParams =>
      var query = Transactions.filter { r => r.id === r.id }

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
      }

      guids.foreach { guids =>
        query = query.filter { _.guid inSet guids }
      }

      createdByAccounts.foreach { createdByAccounts =>
        query = query.filter { _.createdByAccount inSet createdByAccounts }
      }

      database.run(query.result).flatMap { records =>
        records2models(records).map { transactions =>
          PaginatedResult.build(transactions, paginationParams, None)
        }
      }
    }
  }

  def createTransaction(
    transactionTypeGuid: UUID,
    accountGuid: UUID,
    amount: BigDecimal,
    timestamp: DateTime,
    adjustedTransactionGuid: Option[UUID],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[Transaction]] = {
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

    val futValOptAdjustedTransactionId = adjustedTransactionGuid.map { adjustedTransactionGuid =>
      val adjustedTransactionsQuery = Transactions.filter { _.guid === adjustedTransactionGuid }
      database.run(adjustedTransactionsQuery.result).map { transactions =>
        transactions.headOption.map { transaction =>
          Validation.success(Some(transaction.id))
        }.getOrElse {
          Validation.failure("adjustedTransactionGuid", "useless.error.unknownGuid", "specified" -> adjustedTransactionGuid.toString)
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(None))
    }

    futValTransactionTypeId.flatMap { valTransactionTypeId =>
      futValAccountId.flatMap { valAccountId =>
        futValOptAdjustedTransactionId.flatMap { valOptAdjustedTransactionId =>
          ValidationUtil.future(valTransactionTypeId ++ valAccountId ++ valOptAdjustedTransactionId) { case ((transactionTypeId, accountId), optAdjustedTransactionId) =>
            val transactions = Transactions.map { r =>
              (r.guid, r.transactionTypeId, r.accountId, r.amount, r.timestamp, r.adjustedTransactionId, r.createdByAccount, r.createdByAccessToken)
            }.returning(Transactions.map(_.id))

            val insert = transactions += (
              UUID.randomUUID,
              transactionTypeId,
              accountId,
              amount,
              new Timestamp(timestamp.getMillis),
              optAdjustedTransactionId,
              accessToken.resourceOwner.guid,
              accessToken.guid
            )

            database.run(insert).flatMap { id =>
              findTransactions(ids = Some(Seq(id))).map { result =>
                result.map(_.items.headOption) match {
                  case Validation.Success(Some(transaction)) => transaction
                  case _ => throw new ResourceUnexpectedlyNotFound("Transaction", id)
                }
              }
            }
          }
        }
      }
    }
  }

  def adjustTransaction(
    transactionGuid: UUID,
    trasanctionTypeGuid: Option[UUID],
    accountGuid: Option[UUID],
    amount: Option[BigDecimal],
    timestamp: Option[DateTime],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[Transaction]] = {
    findTransactions(guids = Some(Seq(transactionGuid))).flatMap { result =>
      result.map(_.items.headOption) match {
        case Validation.Success(Some(transaction)) => createTransaction(
          transactionTypeGuid = trasanctionTypeGuid.getOrElse(transaction.transactionTypeGuid),
          accountGuid = accountGuid.getOrElse(transaction.accountGuid),
          amount = amount.getOrElse(transaction.amount),
          timestamp = timestamp.getOrElse(transaction.timestamp),
          adjustedTransactionGuid = Some(transaction.guid),
          accessToken = accessToken
        ).flatMap { result =>
          result.fold(
            errors => Future.successful(Validation.failure(errors)),
            transaction => deleteTransaction(transactionGuid, accessToken).map { _ =>
              Validation.success(transaction)
            }
          )
        }
        case Validation.Success(None) => Future.successful {
          Validation.failure("transactionGuid", "useless.error.unknownGuid", "specified" -> transactionGuid.toString)
        }
        case f: Validation.Failure[_] => throw new UnexpectedValidationFailure(f)
      }
    }
  }

  def confirmTransaction(
    transactionGuid: UUID,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[Transaction]] = {
    val transactionQuery = Transactions.filter(_.guid === transactionGuid)
    val futValTransactionId = database.run(transactionQuery.result).map { transactions =>
      transactions.headOption.map { transaction =>
        Validation.success(transaction.id)
      }.getOrElse {
        Validation.failure("transactionGuid", "useless.error.unknownGuid", "specified" -> transactionGuid.toString)
      }
    }

    futValTransactionId.flatMap { valTransactionId =>
      ValidationUtil.future(valTransactionId) { transactionId =>
        val confirmations = TransactionConfirmations.map { r =>
          (r.guid, r.transactionId, r.createdByAccount, r.createdByAccessToken)
        }.returning(TransactionConfirmations.map(_.id))

        val insert = confirmations += (UUID.randomUUID, transactionId, accessToken.resourceOwner.guid, accessToken.guid)

        database.run(insert).flatMap { _ =>
          findTransactions(ids = Some(Seq(transactionId))).map { result =>
            result.map(_.items.headOption) match {
              case Validation.Success(Some(transaction)) => transaction
              case _ => throw new ResourceUnexpectedlyNotFound("Transaction", transactionId)
            }
          }
        }
      }
    }
  }

  def deleteTransaction(
    transactionGuid: UUID,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Boolean] = {
    val now = new Timestamp((new Date).getTime)

    val query = Transactions.filter { transaction =>
      transaction.guid === transactionGuid &&
      transaction.deletedAt.isEmpty
    }.map { transaction =>
      (transaction.deletedAt, transaction.deletedByAccount, transaction.deletedByAccessToken)
    }.update((Some(now), Some(accessToken.resourceOwner.guid), Some(accessToken.guid)))

    database.run(query).map { result =>
      if (result > 0) true else false
    }
  }

}
