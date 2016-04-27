package test.budget.integration.aggregates

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.LocalDate

import models.budget.Account
import models.budget.aggregates.AccountHistoryInterval
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class AccountHistorySpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /aggregates/accountHistory/:uuid" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val account = TestService.createAccount()
      val response = await { unauthentictedRequest(s"/aggregates/accountHistory/${account.guid}").get }
      response.status mustBe UNAUTHORIZED
    }

    "return daily intervals for the specified account, within the specified dates" in {
      TestService.deleteAccounts()
      val account = TestService.createAccount(initialBalance = 50.0)

      def addTxn(amount: BigDecimal, daysAgo: Int) = {
        TestService.createTransaction(
          accountGuid = account.guid,
          amount = amount,
          date = LocalDate.now.minusDays(daysAgo)
        )
      }

      addTxn(100.0, 4)
      addTxn(-75.0, 2)

      val response = await {
        authenticatedRequest(s"/aggregates/accountHistory/${account.guid}").
          withQueryString(
            "intervalType" -> "day",
            "start" -> LocalDate.now.minusDays(3).toString,
            "end" -> LocalDate.now.toString
          ).
          get()
      }

      response.status mustBe OK
      val intervals = response.json.as[Seq[AccountHistoryInterval]]
      intervals.length mustBe 3

      def getInterval(daysAgo: Int) = intervals.find { interval =>
        interval.accountGuid == account.guid &&
        interval.start == LocalDate.now.minusDays(daysAgo)
      }.get

      val interval1 = getInterval(3)
      interval1.initialBalance mustBe 150.0
      interval1.transactionAmountTotal mustBe 0.0
      interval1.transactionCount mustBe 0

      val interval2 = getInterval(2)
      interval2.initialBalance mustBe 150.0
      interval2.transactionAmountTotal mustBe -75.0
      interval2.transactionCount mustBe 1

      val interval3 = getInterval(1)
      interval3.initialBalance mustBe 75.0
      interval3.transactionAmountTotal mustBe 0.0
      interval3.transactionCount mustBe 0
    }

  }

}
