package test.budget.integration.aggregates

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.LocalDate

import models.budget.JsonImplicits._
import models.budget.aggregates.MonthRollup
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class MonthRollupsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /aggregates/monthRollups" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/aggregates/monthRollups").get }
      response.status mustBe UNAUTHORIZED
    }

    "return rollups for all the months that include transactions for the requestor" in {
      TestService.deleteAccounts()
      TestService.deleteTransactions()
      val account1 = TestService.createAccount(contextGuid = TestService.myContext.guid)
      val account2 = TestService.createAccount(contextGuid = TestService.myContext.guid)
      val sharedAccount = TestService.createAccount(contextGuid = TestService.sharedContext.guid)
      val otherAccount = TestService.createAccount(contextGuid = TestService.otherContext.guid)

      TestService.createTransaction(
        accountGuid = account2.guid,
        date = new LocalDate(2016, 1, 15)
      )

      TestService.createTransaction(
        accountGuid = sharedAccount.guid,
        date = new LocalDate(2016, 3, 15)
      )

      TestService.createTransaction(
        accountGuid = account1.guid,
        date = new LocalDate(2015, 12, 15)
      )

      TestService.createTransaction(
        accountGuid = account1.guid,
        date = new LocalDate(2015, 12, 15)
      )

      TestService.createTransaction(
        accountGuid = otherAccount.guid,
        date = new LocalDate(2016, 2, 15)
      )

      val response = await {
        authenticatedRequest("/aggregates/monthRollups").get()
      }

      response.status mustBe OK
      val rollups = response.json.as[Seq[MonthRollup]]
      rollups.length mustBe 3

      val march = rollups(0)
      march.year mustBe 2016
      march.month mustBe 3
      march.transactionCount mustBe 1

      val january = rollups(1)
      january.year mustBe 2016
      january.month mustBe 1
      january.transactionCount mustBe 1

      val december = rollups(2)
      december.year mustBe 2015
      december.month mustBe 12
      december.transactionCount mustBe 2
    }

    "return rollups only for the specified context" in {
      TestService.deleteAccounts()
      TestService.deleteTransactions()
      val account = TestService.createAccount(contextGuid = TestService.myContext.guid)
      val sharedAccount = TestService.createAccount(contextGuid = TestService.sharedContext.guid)

      TestService.createTransaction(
        accountGuid = account.guid,
        date = new LocalDate(2015, 12, 15)
      )

      TestService.createTransaction(
        accountGuid = sharedAccount.guid,
        date = new LocalDate(2016, 1, 15)
      )

      val response = await {
        authenticatedRequest("/aggregates/monthRollups").
          withQueryString(
            "contextGuid" -> TestService.myContext.guid.toString
          ).get()
      }

      response.status mustBe OK
      val rollups = response.json.as[Seq[MonthRollup]]
      rollups.length mustBe 1

      rollups(0).year mustBe 2015
      rollups(0).month mustBe 12
    }

  }

}
