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
      val account = TestService.createAccount()

      TestService.createTransaction(
        accountGuid = account.guid,
        date = new LocalDate(2015, 12, 15)
      )
      TestService.createTransaction(
        accountGuid = account.guid,
        date = new LocalDate(2016, 2, 15)
      )

      val response = await {
        authenticatedRequest("/aggregates/monthRollups").get()
      }

      response.status mustBe OK
      val rollups = response.json.as[Seq[MonthRollup]]
      rollups.length mustBe 2
      rollups(0).year mustBe 2015
      rollups(0).month mustBe 12
      rollups(1).year mustBe 2016
      rollups(1).month mustBe 2
    }

  }

}
