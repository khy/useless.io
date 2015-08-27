package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.{Play, Application}
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import io.useless.accesstoken.AccessToken
import io.useless.account.{User, PublicUser}
import io.useless.client.account.AccountClient
import io.useless.validation._
import io.useless.util.configuration.RichConfiguration._

import models.budget.{Account, AccountType}
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util.ResourceUnexpectedlyNotFound

object AccountsService {

  def default()(implicit app: Application) = {
    val authGuid = Play.configuration.underlying.getUuid("budget.accessTokenGuid")

    new AccountsService(
      accountClient = AccountClient.instance(authGuid)
    )
  }

}

class AccountsService(
  accountClient: AccountClient
) extends DatabaseAccessor {

  def records2models(records: Seq[AccountRecord])(implicit ec: ExecutionContext): Future[Seq[Account]] = {
    val userGuids = records.map(_.createdByAccount)

    getUsers(userGuids).map { users =>
      records.map { record =>
        Account(
          guid = record.guid,
          accountType = AccountType(record.typeKey),
          name = record.name,
          initialBalance = record.initialBalance,
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(AnonUser),
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
      (a.guid, a.typeKey, a.name, a.initialBalance, a.createdByAccessToken, a.createdByAccount)
    }.returning(Accounts.map(_.id))

    val insert = accounts += (UUID.randomUUID, accountType.key, name, initialBalance, accessToken.guid, accessToken.resourceOwner.guid)

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

  private def getUsers(guids: Seq[UUID])(implicit ec: ExecutionContext): Future[Seq[User]] = {
    val userOptFuts = guids.map { guid =>
      accountClient.getAccount(guid).map { optAccount =>
        optAccount match {
          case Some(user: User) => Some(user)
          case _ => None
        }
      }
    }

    Future.sequence(userOptFuts).map { userOpts =>
      userOpts.filter(_.isDefined).map(_.get)
    }
  }

  private val AnonUser: User = new PublicUser(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    handle = "anon",
    name = None
  )

}
