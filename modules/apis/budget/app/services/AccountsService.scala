package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
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

    usersHelper.getUsers(userGuids).map { users =>
      records.map { record =>
        Account(
          guid = record.guid,
          accountType = AccountType(record.typeKey),
          name = record.name,
          initialBalance = record.initialBalance,
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findAccounts(
    ids: Option[Seq[Long]] = None
  )(implicit ec: ExecutionContext): Future[Seq[Account]] = {
    var query = Accounts.filter { a => a.id === a.id }

    ids.foreach { ids =>
      query = query.filter { _.id inSet ids }
    }

    database.run(query.result).flatMap(records2models)
  }

  def createAccount(
    accountType: AccountType,
    name: String,
    initialBalance: Option[BigDecimal],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[Account]] = {
    val accounts = Accounts.map { a =>
      (a.guid, a.typeKey, a.name, a.initialBalance, a.createdByAccount)
    }.returning(Accounts.map(_.id))

    val insert = accounts += (UUID.randomUUID, accountType.key, name, initialBalance, accessToken.resourceOwner.guid)

    database.run(insert).flatMap { id =>
      findAccounts(ids = Some(Seq(id))).map { accounts =>
        accounts.headOption.map { account =>
          Validation.success(account)
        }.getOrElse {
          throw new ResourceUnexpectedlyNotFound("Account", id)
        }
      }
    }
  }

}
