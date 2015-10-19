package services.budget

import java.util.{Date, UUID}
import java.sql.Timestamp
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
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

  def getProjections(
    date: LocalDate,
    coreAccountGuid: UUID,
    accountGuids: Option[Seq[UUID]] = None
  )(implicit ec: ExecutionContext): Future[Validation[Seq[Projection]]] = {
    accountsService.findAccounts(createdByAccounts = Some(Seq(coreAccountGuid))).flatMap { result =>
      val accounts = result.toSuccess.value.items

      val sumsQuery = PlannedTransactions.join(Accounts).on { case (plannedTransaction, account) =>
        plannedTransaction.accountId === account.id
      }.filter { case (plannedTransaction, account) =>
        account.guid.inSet(accounts.map(_.guid)) &&
        plannedTransaction.minTimestamp <= new Timestamp(date.toDateTimeAtStartOfDay.getMillis) &&
        plannedTransaction.maxTimestamp >= new Timestamp(date.toDateTimeAtStartOfDay.getMillis) &&
        plannedTransaction.deletedAt.isEmpty
      }.groupBy { case (plannedTransaction, account) =>
        account.guid
      }.map { case (accountGuid, group) => (
        accountGuid,
        group.map { case (plannedTransaction, _) => plannedTransaction.minAmount }.sum,
        group.map { case (plannedTransaction, _) => plannedTransaction.maxAmount }.sum
      )}

      database.run(sumsQuery.result).map { sums =>
        val projections = accounts.map { account =>
          val minPlannedAmount: BigDecimal = sums.
            find { case (accountGuid, _, _) => accountGuid == account.guid }.
            flatMap { case (_, minAmount, _) => minAmount }.
            getOrElse(0.0)

          val maxPlannedAmount: BigDecimal = sums.
            find { case (accountGuid, _, _) => accountGuid == account.guid }.
            flatMap { case (_, _, maxAmount) => maxAmount }.
            getOrElse(0.0)

          Projection(
            account = account,
            date = date,
            minAmount = (account.balance + minPlannedAmount),
            maxAmount = (account.balance + maxPlannedAmount)
          )
        }

        Validation.success(projections)
      }
    }
  }

}
