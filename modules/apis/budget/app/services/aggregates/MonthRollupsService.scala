package services.budget.aggregates

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.LocalDate
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import db.budget.util.DatabaseAccessor
import db.budget.{Transactions, TransactionTypes}
import services.budget.TransactionTypesService
import services.budget.util.ResourceUnexpectedlyNotFound
import models.budget.aggregates.MonthRollup

object MonthRollupsService {

  def default() = {
    new MonthRollupsService
  }
}

class MonthRollupsService() extends DatabaseAccessor {

  def getMonthRollups(
    contextGuids: Option[Seq[UUID]],
    accessToken: AccessToken,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[MonthRollup]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var query = Transactions.filter { transaction =>
        transaction.deletedAt.isEmpty
      }

      database.run(query.result).map { transactions =>
        val rollups = transactions.groupBy { transaction =>
          val createdAt = new LocalDate(transaction.date)
          (createdAt.getYear, createdAt.getMonthOfYear)
        }.map { case ((year, month), group) =>
          MonthRollup(UUID.randomUUID, year, month)
        }.toSeq

        PaginatedResult.build(rollups, paginationParams, None)
      }
    }
  }

}
