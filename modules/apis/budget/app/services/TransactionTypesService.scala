package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.validation._

import models.budget.{TransactionType, TransactionClass}
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util.{UsersHelper, ResourceUnexpectedlyNotFound}

object TransactionTypesService {

  def default()(implicit app: Application) = new TransactionTypesService(UsersHelper.default())

}

class TransactionTypesService(
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(records: Seq[TransactionTypeRecord])(implicit ec: ExecutionContext): Future[Seq[TransactionType]] = {
    val userGuids = records.map(_.createdByAccount)
    val futUsers = usersHelper.getUsers(userGuids)

    val query = Accounts.filter { _.id inSet records.map(_.accountId) }
    val futAccounts = database.run(query.result)

    for {
      users <- futUsers
      accounts <- futAccounts
    } yield {
      records.map { record =>
        TransactionType(
          guid = record.guid,
          accountGuid = accounts.find(_.id == record.accountId).map(_.guid).getOrElse {
            throw new ResourceUnexpectedlyNotFound("Account", record.accountId)
          },
          transactionClass = TransactionClass(record.transactionClassKey),
          name = record.name,
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findTransactionTypes(
    ids: Option[Seq[Long]] = None
  )(implicit ec: ExecutionContext): Future[Seq[TransactionType]] = {
    var query = TransactionTypes.filter { r => r.id === r.id }

    ids.foreach { ids =>
      query = query.filter { _.id inSet ids }
    }

    database.run(query.result).flatMap(records2models)
  }

  def createTransactionType(
    accountGuid: UUID,
    transactionClass: TransactionClass,
    name: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[TransactionType]] = {
    val query = Accounts.filter { account => account.guid === accountGuid }

    database.run(query.result).flatMap { accounts =>
      accounts.headOption.map { account =>
        val transactionTypes = TransactionTypes.map { r =>
          (r.guid, r.name, r.accountId, r.transactionClassKey, r.createdByAccount, r.createdByAccessToken)
        }.returning(TransactionTypes.map(_.id))

        val insert = transactionTypes += (UUID.randomUUID, name, account.id, transactionClass.key, accessToken.resourceOwner.guid, accessToken.guid)

        database.run(insert).flatMap { id =>
          findTransactionTypes(ids = Some(Seq(id))).map { transactionTypes =>
            transactionTypes.headOption.map { transactionGroup =>
              Validation.success(transactionGroup)
            }.getOrElse {
              throw new ResourceUnexpectedlyNotFound("TransactionGroup", id)
            }
          }
        }
      }.getOrElse {
        Future.successful(Validation.failure("accountGuid", "useless.error.unknownGuid", "specified" -> accountGuid.toString))
      }
    }
  }

}
