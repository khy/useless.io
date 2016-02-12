package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget.{Account, AccountType}
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util.{UsersHelper, ResourceUnexpectedlyNotFound}

object AccountsService {

  def default()(implicit app: Application) = new AccountsService(UsersHelper.default())

}

class AccountsService(
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(records: Seq[AccountRecord])(implicit ec: ExecutionContext): Future[Seq[Account]] = {
    val userGuids = records.map(_.createdByAccount)
    val futUsers = usersHelper.getUsers(userGuids)

    val transactionSumsQuery = Transactions.filter { transaction =>
      transaction.accountId.inSet(records.map(_.id)) &&
      transaction.deletedAt.isEmpty
    }.groupBy { transaction =>
      transaction.accountId
    }.map { case (accountId, transaction) =>
      (accountId, transaction.map(_.amount).sum)
    }
    val futTransactionSums = database.run(transactionSumsQuery.result)

    val contextsQuery = Contexts.filter { context =>
      context.id.inSet(records.map(_.contextId)) &&
      context.deletedAt.isEmpty
    }
    val futContexts = database.run(contextsQuery.result)

    for {
      users <- futUsers
      transactionSums <- futTransactionSums
      contexts <- futContexts
    } yield {
      records.map { record =>
        val transactionSum: BigDecimal = transactionSums.
          find { case (accountId, _) => accountId == record.id }.
          flatMap { case (_, sum) => sum }.
          getOrElse(0.0)

        val contextGuid = contexts.find(_.id == record.contextId).map(_.guid).getOrElse {
          throw new ResourceUnexpectedlyNotFound("Context", record.contextId)
        }

        Account(
          guid = record.guid,
          contextGuid = contextGuid,
          accountType = AccountType(record.accountTypeKey),
          name = record.name,
          initialBalance = record.initialBalance,
          balance = record.initialBalance + transactionSum,
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findAccounts(
    ids: Option[Seq[Long]] = None,
    userGuids: Option[Seq[UUID]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[Account]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.future(valPaginationParams) { paginationParams =>
      var query = Accounts.filter { a => a.id === a.id }

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
      }

      userGuids.foreach { userGuids =>
        val subQuery = ContextUsers.filter { contextUser =>
          contextUser.userGuid.inSet(userGuids) && contextUser.deletedAt.isEmpty
        }.map(_.contextId)

        query = query.filter { _.contextId in subQuery }
      }

      database.run(query.result).flatMap { records =>
        records2models(records).map { accounts =>
          PaginatedResult.build(accounts, paginationParams, None)
        }
      }
    }
  }

  def createAccount(
    contextGuid: UUID,
    accountType: AccountType,
    name: String,
    initialBalance: BigDecimal,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[Account]] = {
    val contextQuery = Contexts.filter { _.guid === contextGuid }
    val futValContextId = database.run(contextQuery.result).map { contexts =>
      contexts.headOption.map { context =>
        Validation.success(context.id)
      }.getOrElse {
        Validation.failure("contextGuid", "useless.error.unknownGuid", "specified" -> contextGuid.toString)
      }
    }

    futValContextId.flatMap { valContextId =>
      ValidationUtil.future(valContextId) { contextId =>
        val accounts = Accounts.map { a =>
          (a.guid, a.contextId, a.accountTypeKey, a.name, a.initialBalance, a.createdByAccount, a.createdByAccessToken)
        }.returning(Accounts.map(_.id))

        val insert = accounts += (UUID.randomUUID, contextId, accountType.key, name, initialBalance, accessToken.resourceOwner.guid, accessToken.guid)

        database.run(insert).flatMap { id =>
          findAccounts(ids = Some(Seq(id))).map { result =>
            result.map(_.items.headOption) match {
              case Validation.Success(Some(account)) => account
              case _ => throw new ResourceUnexpectedlyNotFound("Account", id)
            }
          }
        }
      }
    }
  }

}
