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

    val transactionTypeQuery = TransactionTypes.filter { transactionType =>
      transactionType.id inSet records.map(_.parentId).filter(_.isDefined).map(_.get)
    }
    val futTransactionTypes = database.run(transactionTypeQuery.result)

    val accountQuery = Accounts.filter { account =>
      account.id inSet records.map(_.accountId).filter(_.isDefined).map(_.get)
    }
    val futAccounts = database.run(accountQuery.result)

    for {
      users <- futUsers
      transactionTypes <- futTransactionTypes
      accounts <- futAccounts
    } yield {
      records.map { record =>
        TransactionType(
          guid = record.guid,
          name = record.name,
          parentGuid = transactionTypes.find { transactionType =>
            record.parentId.map(_ == transactionType.id).getOrElse(false)
          }.map(_.guid),
          accountGuid = accounts.find { account =>
            record.accountId.map(_ == account.id).getOrElse(false)
          }.map(_.guid),
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
    name: String,
    parentGuid: Option[UUID],
    accountGuid: Option[UUID],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[TransactionType]] = {
    val futValOptParentId = parentGuid.map { parentGuid =>
      val transactionTypeQuery = TransactionTypes.filter { _.guid === parentGuid }
      database.run(transactionTypeQuery.result).map { transactionTypes =>
        transactionTypes.headOption.map { transactionType =>
          Validation.success(Some(transactionType.id))
        }.getOrElse {
          Validation.failure("parentGuid", "useless.error.unknownGuid", "specified" -> parentGuid.toString)
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(None))
    }

    val futValOptAccountId = accountGuid.map { accountGuid =>
      val accountQuery = Accounts.filter { _.guid === accountGuid }
      database.run(accountQuery.result).map { accounts =>
        accounts.headOption.map { account =>
          Validation.success(Some(account.id))
        }.getOrElse {
          Validation.failure("accountGuid", "useless.error.unknownGuid", "specified" -> accountGuid.toString)
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(None))
    }

    futValOptParentId.flatMap { valOptParentId =>
      futValOptAccountId.flatMap { valOptAccountId =>
        ValidationUtil.future(valOptParentId ++ valOptAccountId) { case (optParentId, optAccountId) =>
          val transactionTypes = TransactionTypes.map { r =>
            (r.guid, r.parentId, r.accountId, r.name, r.createdByAccount, r.createdByAccessToken)
          }.returning(TransactionTypes.map(_.id))

          val insert = transactionTypes += ((UUID.randomUUID, optParentId, optAccountId, name, accessToken.resourceOwner.guid, accessToken.guid))

          database.run(insert).flatMap { id =>
            findTransactionTypes(ids = Some(Seq(id))).map { transactionTypes =>
              transactionTypes.headOption.getOrElse {
                throw new ResourceUnexpectedlyNotFound("TransactionGroup", id)
              }
            }
          }
        }
      }
    }
  }

}
