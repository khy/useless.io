package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.validation._

import models.budget.{TransactionGroup, TransactionType}
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util.{UsersHelper, ResourceUnexpectedlyNotFound}

object TransactionGroupsService {

  def default()(implicit app: Application) = new TransactionGroupsService(UsersHelper.default())

}

class TransactionGroupsService(
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(records: Seq[TransactionGroupRecord])(implicit ec: ExecutionContext): Future[Seq[TransactionGroup]] = {
    val userGuids = records.map(_.createdByAccount)
    val futUsers = usersHelper.getUsers(userGuids)

    val query = Accounts.filter { _.id inSet records.map(_.accountId) }
    val futAccounts = database.run(query.result)

    for {
      users <- futUsers
      accounts <- futAccounts
    } yield {
      records.map { record =>
        TransactionGroup(
          guid = record.guid,
          accountGuid = accounts.find(_.id == record.accountId).map(_.guid).getOrElse {
            throw new ResourceUnexpectedlyNotFound("Account", record.accountId)
          },
          transactionType = TransactionType(record.transactionTypeKey),
          name = record.name,
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findTransactionGroups(
    ids: Option[Seq[Long]] = None
  )(implicit ec: ExecutionContext): Future[Seq[TransactionGroup]] = {
    var query = TransactionGroups.filter { r => r.id === r.id }

    ids.foreach { ids =>
      query = query.filter { _.id inSet ids }
    }

    database.run(query.result).flatMap(records2models)
  }

  def createTransactionGroups(
    accountGuid: UUID,
    transactionType: TransactionType,
    name: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[TransactionGroup]] = {
    val query = Accounts.filter { account => account.guid === accountGuid }

    database.run(query.result).flatMap { accounts =>
      accounts.headOption.map { account =>
        val projections = TransactionGroups.map { r =>
          (r.guid, r.name, r.accountId, r.transactionTypeKey, r.createdByAccount)
        }.returning(TransactionGroups.map(_.id))

        val insert = projections += (UUID.randomUUID, name, account.id, transactionType.key, accessToken.resourceOwner.guid)

        database.run(insert).flatMap { id =>
          findTransactionGroups(ids = Some(Seq(id))).map { transactionGroups =>
            transactionGroups.headOption.map { transactionGroup =>
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
