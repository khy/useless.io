package services.budget

import java.util.UUID
import java.sql.Timestamp
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
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

    val transactionGroupsQuery = TransactionGroups.filter { _.id inSet records.map(_.transactionGroupId) }
    val futTransactionGroups = database.run(transactionGroupsQuery.result)

    val projectionsQuery = Projections.filter { _.id inSet records.map(_.projectionId).filter(_.isDefined).map(_.get) }
    val futProjections = database.run(projectionsQuery.result)

    for {
      users <- futUsers
      transactionGroups <- futTransactionGroups
      projections <- futProjections
    } yield {
      records.map { record =>
        Transaction(
          guid = record.guid,
          transactionGroupGuid = transactionGroups.find(_.id == record.transactionGroupId).map(_.guid).getOrElse {
            throw new ResourceUnexpectedlyNotFound("TransactionGroup", record.transactionGroupId)
          },
          amount = record.amount,
          timestamp = new DateTime(record.timestamp),
          projectionGuid = record.projectionId.flatMap { projectionId =>
            projections.find(_.id == projectionId).map(_.guid)
          },
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findTransactions(
    ids: Option[Seq[Long]] = None
  )(implicit ec: ExecutionContext): Future[Seq[Transaction]] = {
    var query = Transactions.filter { r => r.id === r.id }

    ids.foreach { ids =>
      query = query.filter { _.id inSet ids }
    }

    database.run(query.result).flatMap(records2models)
  }

  def createTransaction(
    transactionGroupUuid: UUID,
    amount: BigDecimal,
    timestamp: DateTime,
    projectionUuid: Option[UUID],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[Transaction]] = {
    val transactionGroupsQuery = TransactionGroups.filter { _.guid === transactionGroupUuid }
    val futValTransactionGroupId = database.run(transactionGroupsQuery.result).map { transactionGroups =>
      transactionGroups.headOption.map { transactionGroup =>
        Validation.success(transactionGroup.id)
      }.getOrElse {
        Validation.failure("transactionGroupUuid", "useless.error.unknownGuid", "specified" -> transactionGroupUuid.toString)
      }
    }

    val futValOptProjectionId = projectionUuid.map { projectionUuid =>
      val projectionsQuery = Projections.filter { _.guid === projectionUuid }
      database.run(projectionsQuery.result).map { projections =>
        projections.headOption.map { projection =>
          Validation.success(Some(projection.id))
        }.getOrElse {
          Validation.failure("projectionUuid", "useless.error.unknownGuid", "specified" -> projectionUuid.toString)
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(None))
    }

    futValTransactionGroupId.flatMap { valTransactionGroupId =>
      futValOptProjectionId.flatMap { valOptProjectionId =>
        ValidationUtil.future(valTransactionGroupId ++ valOptProjectionId) { case (transactionGroupId, optProjectionId) =>
          val transactions = Transactions.map { r =>
            (r.guid, r.transactionGroupId, r.amount, r.timestamp, r.projectionId, r.createdByAccount)
          }.returning(Transactions.map(_.id))

          val insert = transactions += (UUID.randomUUID, transactionGroupId, amount, new Timestamp(timestamp.getMillis), optProjectionId, accessToken.resourceOwner.guid)

          database.run(insert).flatMap { id =>
            findTransactions(ids = Some(Seq(id))).map { transactions =>
              transactions.headOption.getOrElse {
                throw new ResourceUnexpectedlyNotFound("Transaction", id)
              }
            }
          }
        }
      }
    }
  }

}
