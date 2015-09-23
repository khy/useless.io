package services.budget

import java.util.UUID
import java.sql.Timestamp
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget.Transaction
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util.{UsersHelper, ResourceUnexpectedlyNotFound}

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

    val accountQuery = Accounts.filter { account =>
      account.id inSet records.map(_.accountId)
    }
    val futAccounts = database.run(accountQuery.result)

    for {
      users <- futUsers
      transactionTypes <- futTransactionTypes
      accounts <- futAccounts
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
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findTransactions(
    ids: Option[Seq[Long]] = None,
    createdByAccounts: Option[Seq[UUID]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[Transaction]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.future(valPaginationParams) { paginationParams =>
      var query = Transactions.filter { r => r.id === r.id }

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
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
    val futValAccountId =database.run(accountQuery.result).map { accounts =>
      accounts.headOption.map { account =>
        Validation.success(account.id)
      }.getOrElse {
        Validation.failure("accountGuid", "useless.error.unknownGuid", "specified" -> accountGuid.toString)
      }
    }

    futValTransactionTypeId.flatMap { valTransactionTypeId =>
      futValAccountId.flatMap { valAccountId =>
        ValidationUtil.future(valTransactionTypeId ++ valAccountId) { case (transactionTypeId, accountId) =>
          val transactions = Transactions.map { r =>
            (r.guid, r.transactionTypeId, r.accountId, r.amount, r.timestamp, r.createdByAccount, r.createdByAccessToken)
          }.returning(Transactions.map(_.id))

          val insert = transactions += (UUID.randomUUID, transactionTypeId, accountId, amount, new Timestamp(timestamp.getMillis), accessToken.resourceOwner.guid, accessToken.guid)

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
