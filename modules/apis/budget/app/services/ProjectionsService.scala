package services.budget

import java.util.{Date, UUID}
import java.sql.Timestamp
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import slick.lifted.Rep
import org.joda.time.LocalDate
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget._
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util._

object ProjectionsService {

  def default()(implicit app: Application) = {
    new ProjectionsService(
      accountsService = AccountsService.default()
    )
  }
}

class ProjectionsService(
  accountsService: AccountsService
) extends DatabaseAccessor {

  /**
   * We're looking for an *absolute* min and max. I.e., given every possible
   * combination of transactions that satisfy the ranges of the accounts'
   * planned transaction, on or before the specified date, which combination
   * results in the lowest and highest balances.
   *
   * So, for both min and max amount sums, include all of the projected
   * transactions that have a date range that is strictly prior to the specified
   * date. For min, also include all of the projected transactions that have a date
   * range that stradles the specified date, and that have a negative minAmount.
   * Include the same for max, except only if they have a positive maxAmount.
   */
  def getProjections(
    date: LocalDate,
    coreAccountGuid: UUID,
    accountGuids: Option[Seq[UUID]] = None
  )(implicit ec: ExecutionContext): Future[Validation[Seq[Projection]]] = {
    accountsService.findAccounts(createdByAccounts = Some(Seq(coreAccountGuid))).flatMap { result =>
      val accounts = result.toSuccess.value.items

      val timestamp = new Timestamp(date.toDateTimeAtStartOfDay.getMillis)

      def amountSumQuery(
        amount: PlannedTransactionsTable => Rep[Option[BigDecimal]]
      )(
        condition: Rep[Option[BigDecimal]] => Rep[Option[Boolean]]
      ) = {
        PlannedTransactions.join(Accounts).on { case (plannedTransaction, account) =>
          plannedTransaction.accountId === account.id
        }.filter { case (pt, account) =>
          account.guid.inSet(accounts.map(_.guid)) && (
            (condition(amount(pt)) && pt.minTimestamp <= timestamp && pt.maxTimestamp >= timestamp) ||
            pt.maxTimestamp <= timestamp
          ) && pt.deletedAt.isEmpty
        }.groupBy { case (_, account) =>
          account.guid
        }.map { case (accountGuid, group) =>
          (accountGuid, group.map { case (pt, _) => amount(pt) }.sum)
        }
      }

      val minAmountSumsQuery = amountSumQuery(_.minAmount) { _ < BigDecimal(0) }
      val maxAmountSumsQuery = amountSumQuery(_.maxAmount) { _ > BigDecimal(0) }

      for {
        minAmountSums <- database.run(minAmountSumsQuery.result)
        maxAmountSums <- database.run(maxAmountSumsQuery.result)
      } yield {
        val projections = accounts.map { account =>
          val minAmountSum: BigDecimal = minAmountSums.
            find { case (accountGuid, _) => accountGuid == account.guid }.
            flatMap { case (_, min) => min }.
            getOrElse(0.0)

          val maxAmountSum: BigDecimal = maxAmountSums.
            find { case (accountGuid, _) => accountGuid == account.guid }.
            flatMap { case (_, max) => max }.
            getOrElse(0.0)

          Projection(
            account = account,
            date = date,
            minAmount = (account.balance + minAmountSum),
            maxAmount = (account.balance + maxAmountSum)
          )
        }

        Validation.success(projections)
      }
    }
  }

}
