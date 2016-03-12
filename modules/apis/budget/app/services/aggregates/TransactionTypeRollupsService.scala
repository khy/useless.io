package services.budget.aggregates

import java.util.UUID
import java.sql.Date
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.LocalDate
import io.useless.accesstoken.AccessToken
import io.useless.validation._

import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.TransactionTypesService
import services.budget.util.ResourceUnexpectedlyNotFound
import models.budget.aggregates.TransactionTypeRollup

object TransactionTypeRollupsService {

  def default()(implicit app: Application) = {
    new TransactionTypeRollupsService(
      TransactionTypesService.default()
    )
  }
}

class TransactionTypeRollupsService(
  transactionTypesService: TransactionTypesService
) extends DatabaseAccessor {

  def getTransactionTypeRollups(
    fromDate: Option[LocalDate],
    toDate: Option[LocalDate],
    userGuids: Option[Seq[UUID]]
  )(implicit ec: ExecutionContext): Future[Validation[Seq[TransactionTypeRollup]]] = {
    var query = TransactionTypes.filter { txnType =>
      txnType.deletedAt.isEmpty
    }

    userGuids.foreach { userGuids =>
      val subQuery = ContextUsers.filter { contextUser =>
        contextUser.userGuid inSet userGuids
      }.map { contextUser =>
        contextUser.contextId
      }

      query = query.filter { txnType =>
        txnType.contextId in subQuery
      }
    }

    database.run(query.result).flatMap { transactionTypeRecords =>
      var query = Transactions.filter { transaction =>
        transaction.transactionTypeId inSet transactionTypeRecords.map(_.id)
      }

      fromDate.foreach { fromDate =>
        query = query.filter { _.date >= new Date(fromDate.toDate.getTime) }
      }

      toDate.foreach { toDate =>
        query = query.filter { _.date < new Date(toDate.toDate.getTime) }
      }

      val aggQuery = query.groupBy(_.transactionTypeId).map { case (transactionTypeId, agg) =>
        (transactionTypeId, agg.map(_.amount).sum)
      }

      for {
        transactionTypeModels <- transactionTypesService.records2models(transactionTypeRecords)
        transactionAmountSums <- database.run(aggQuery.result)
      } yield {
        val rollups = transactionAmountSums.map { case (transactionTypeId, optAmountSum) =>
          val transactionType = transactionTypeRecords.find(_.id == transactionTypeId).flatMap { record =>
            transactionTypeModels.find(_.guid == record.guid)
          }.getOrElse {
            throw new ResourceUnexpectedlyNotFound("TransactionType", transactionTypeId)
          }

          TransactionTypeRollup(transactionType, fromDate, toDate, optAmountSum.getOrElse(0.0))
        }

        Validation.success(rollups)
      }
    }
  }

}
