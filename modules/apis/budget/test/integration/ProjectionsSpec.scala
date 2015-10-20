package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.{DateTime, LocalDate}
import io.useless.play.json.DateTimeJson._

import models.budget.Projection
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class ProjectionsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /projections" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/projections").get() }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict if the 'date' query parameter is missing" in {
      val response = await { authenticatedRequest("/projections").get() }
      response.status mustBe CONFLICT
    }

    "return a projection for the specified 'date', for all the requestor's accounts" in {
      TestService.deleteAccounts()
      val account1 = TestService.createAccount(initialBalance = 50.0)
      val account2 = TestService.createAccount(initialBalance = 25.0)

      TestService.createTransaction(accountGuid = account1.guid, amount = 100.0)
      TestService.createTransaction(accountGuid = account2.guid, amount = -75.0)

      // Because entire range is prior to date below, both min and max count it
      TestService.createPlannedTransaction(
        accountGuid = account1.guid,
        minAmount = Some(25.0),
        maxAmount = Some(50.0),
        minTimestamp = Some(DateTime.now.plusDays(3)),
        maxTimestamp = Some(DateTime.now.plusDays(5))
      )

      // Because range stradles date and min is negative,
      // min counts it but max doesn't
      TestService.createPlannedTransaction(
        accountGuid = account1.guid,
        minAmount = Some(-120.0),
        maxAmount = Some(-100.0),
        minTimestamp = Some(DateTime.now.plusDays(9)),
        maxTimestamp = Some(DateTime.now.plusDays(11))
      )

      // Because range stradles date and max is positive,
      // max counts it but min doesn't
      TestService.createPlannedTransaction(
        accountGuid = account1.guid,
        minAmount = Some(80.0),
        maxAmount = Some(90.0),
        minTimestamp = Some(DateTime.now.plusDays(9)),
        maxTimestamp = Some(DateTime.now.plusDays(11))
      )

      // Because range is past date, neither min or max count it
      TestService.createPlannedTransaction(
        accountGuid = account1.guid,
        minAmount = Some(60.0),
        maxAmount = Some(70.0),
        minTimestamp = Some(DateTime.now.plusDays(11)),
        maxTimestamp = Some(DateTime.now.plusDays(13))
      )

      TestService.createPlannedTransaction(
        accountGuid = account2.guid,
        minAmount = Some(-75.0),
        maxAmount = Some(-50.0),
        minTimestamp = Some(DateTime.now.plusDays(7)),
        maxTimestamp = Some(DateTime.now.plusDays(11))
      )

      TestService.createPlannedTransaction(
        accountGuid = account2.guid,
        minAmount = Some(-35.0),
        maxAmount = Some(-25.0),
        minTimestamp = Some(DateTime.now.plusDays(5)),
        maxTimestamp = Some(DateTime.now.plusDays(5))
      )

      val response = await {
        authenticatedRequest("/projections").
          withQueryString("date" -> LocalDate.now.plusDays(10).toString).
          get()
      }

      response.status mustBe OK
      val projections = response.json.as[Seq[Projection]]

      val projection1 = projections.find(_.account.guid == account1.guid).get
      projection1.minBalance mustBe 55.0
      projection1.maxBalance mustBe 290.0

      val projection2 = projections.find(_.account.guid == account2.guid).get
      projection2.minBalance mustBe -160.0
      projection2.maxBalance mustBe -75.0
    }

  }

}
