package services.budget.aggregates

import java.util.UUID
import java.sql
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.LocalDate
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import db.budget._
import db.budget.util.DatabaseAccessor
import models.budget.IntervalType
import models.budget.aggregates.AccountHistoryInterval

object AccountHistoryService {

  def default() = {
    new AccountHistoryService
  }
}

class AccountHistoryService() extends DatabaseAccessor {

  def getAccountHistory(
    accountGuid: UUID,
    start: Option[LocalDate] = None,
    end: Option[LocalDate] = None,
    intervalType: Option[IntervalType] = None,
    userGuid: Option[UUID] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[AccountHistoryInterval]]] = {
    ValidationUtil.mapFuture(PaginationParams.build(rawPaginationParams)) { paginationParams =>
      var query = Transactions.filter { transaction =>
        val subQuery = Accounts.filter { _.guid === accountGuid }.map(_.id)
        transaction.accountId.in(subQuery) &&
        transaction.deletedAt.isEmpty
      }

      start.foreach { start =>
        query = query.filter { transaction =>
          transaction.date >= new sql.Date(start.toDateTimeAtStartOfDay.getMillis)
        }
      }

      end.foreach { end =>
        query = query.filter { transaction =>
          transaction.date < new sql.Date(end.toDateTimeAtStartOfDay.getMillis)
        }
      }

      database.run(query.result).flatMap { transactions =>
        val sortedTransactions = transactions.sortWith { case (a, b) =>
          a.date.getTime < b.date.getTime
        }

        val optFutIntervals = for {
          start <- start.orElse {
            sortedTransactions.headOption.map { txn => new LocalDate(txn.date) }
          }
          end <- end.orElse {
            sortedTransactions.lastOption.map { txn => new LocalDate(txn.date) }
          }
        } yield {
          getInitialAccountBalance(accountGuid, start).map { initialBalance =>
            val _intervalType = intervalType.getOrElse(IntervalType.Day)
            var intervals = Seq.empty[AccountHistoryInterval]
            var intervalEnd = start
            var nextInitialBalance = initialBalance

            while (intervalEnd.isBefore(end)) {
              val intervalStart = intervalEnd
              intervalEnd = getIntervalEnd(intervalStart, _intervalType)

              val intervalTransactions = transactions.filter { transaction =>
                transaction.date.getTime >= intervalStart.toDateTimeAtStartOfDay.getMillis &&
                transaction.date.getTime < intervalEnd.toDateTimeAtStartOfDay.getMillis
              }

              val transactionAmountTotal = intervalTransactions.map(_.amount).sum
              val initialBalance = nextInitialBalance
              nextInitialBalance = initialBalance + transactionAmountTotal

              intervals = intervals :+ AccountHistoryInterval(
                guid = UUID.randomUUID,
                accountGuid = accountGuid,
                start = intervalStart,
                end = intervalEnd,
                initialBalance = initialBalance,
                transactionAmountTotal = transactionAmountTotal,
                transactionCount = intervalTransactions.length
              )
            }

            intervals
          }
        }

        optFutIntervals.getOrElse(Future.successful(Seq.empty)).map { intervals =>
          PaginatedResult.build(intervals, paginationParams, None)
        }
      }
    }
  }

  private def getIntervalEnd(
    intervalStart: LocalDate,
    intervalType: IntervalType
  ): LocalDate = intervalType match {
    case IntervalType.Day => intervalStart.plusDays(1)
    case _ => intervalStart.plusDays(1)
  }

  private def getInitialAccountBalance(
    accountGuid: UUID,
    date: LocalDate
  )(implicit ec: ExecutionContext): Future[BigDecimal] = {
    val initialBalanceQuery = Accounts.filter { _.guid === accountGuid }.map(_.initialBalance)

    var sumQuery = Transactions.filter { transaction =>
      val subQuery = Accounts.filter { _.guid === accountGuid }.map(_.id)
      transaction.accountId.in(subQuery) &&
      transaction.date < new sql.Date(date.toDateTimeAtStartOfDay.getMillis) &&
      transaction.deletedAt.isEmpty
    }.map { transaction =>
      transaction.amount
    }.sum

    val futInitialBalance = database.run(initialBalanceQuery.result)
    val futSum = database.run(sumQuery.result)

    for {
      initialBalance <- futInitialBalance
      optSum <- futSum
    } yield {
      initialBalance.headOption.getOrElse(BigDecimal(0)) +
      optSum.getOrElse(BigDecimal(0))
    }
  }

}
