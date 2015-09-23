package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget.TransactionType
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

    for {
      users <- futUsers
      transactionTypes <- futTransactionTypes
    } yield {
      records.map { record =>
        TransactionType(
          guid = record.guid,
          name = record.name,
          parentGuid = transactionTypes.find { transactionType =>
            record.parentId.map(_ == transactionType.id).getOrElse(false)
          }.map(_.guid),
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findTransactionTypes(
    ids: Option[Seq[Long]] = None,
    names: Option[Seq[String]] = None,
    createdByAccounts: Option[Seq[UUID]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[TransactionType]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.future(valPaginationParams) { paginationParams =>
      var query = TransactionTypes.filter { r => r.id === r.id }

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
      }

      names.foreach { names =>
        query = query.filter { _.name inSet names }
      }

      database.run(query.result).flatMap { records =>
        records2models(records).map { transactionTypes =>
          PaginatedResult.build(transactionTypes, paginationParams, None)
        }
      }
    }
  }

  def createTransactionType(
    name: String,
    parentGuid: Option[UUID],
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

    futValOptParentId.flatMap { valOptParentId =>
      ValidationUtil.future(valOptParentId) { case (optParentId) =>
        val transactionTypes = TransactionTypes.map { r =>
          (r.guid, r.parentId, r.name, r.createdByAccount, r.createdByAccessToken)
        }.returning(TransactionTypes.map(_.id))

        val insert = transactionTypes += ((UUID.randomUUID, optParentId, name, accessToken.resourceOwner.guid, accessToken.guid))

        database.run(insert).flatMap { id =>
          findTransactionTypes(ids = Some(Seq(id))).map { result =>
            result.map(_.items.headOption) match {
              case Validation.Success(Some(transactionType)) => transactionType
              case _ => throw new ResourceUnexpectedlyNotFound("TransactionGroup", id)
            }
          }
        }
      }
    }
  }

}
