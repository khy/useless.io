package services.budget.aggregates

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.LocalDate
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import db.budget._
import db.budget.util.DatabaseAccessor
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
    userGuids: Option[Seq[UUID]],
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[MonthRollup]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var query = Transactions.filter { transaction =>
        transaction.deletedAt.isEmpty
      }

      contextGuids.foreach { contextGuids =>
        val subQuery = Accounts.join(Contexts).on { case (account, context) =>
          account.contextId === context.id && context.deletedAt.isEmpty
        }.filter { case (_, context) =>
          context.guid inSet contextGuids
        }.map { case (account, _) =>
          account.id
        }

        query = query.filter { txn=>
          txn.accountId in subQuery
        }
      }

      userGuids.foreach { userGuids =>
        val subQuery = Accounts.join(ContextUsers).on { case (account, contextUser) =>
          account.contextId === contextUser.contextId && contextUser.deletedAt.isEmpty
        }.filter { case (_, contextUser) =>
          contextUser.userGuid inSet userGuids
        }.map { case (account, _) =>
          account.id
        }

        query = query.filter { txn =>
          txn.accountId in subQuery
        }
      }

      database.run(query.result).map { transactions =>
        val rollups = transactions.groupBy { transaction =>
          val createdAt = new LocalDate(transaction.date)
          (createdAt.getYear, createdAt.getMonthOfYear)
        }.map { case ((year, month), group) =>
          MonthRollup(UUID.randomUUID, year, month)
        }.toSeq.sortWith { case (a, b) =>
          a.year > b.year || (a.year == b.year && a.month > b.month)
        }

        PaginatedResult.build(rollups, paginationParams, None)
      }
    }
  }

}
