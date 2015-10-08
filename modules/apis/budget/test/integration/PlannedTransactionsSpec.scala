package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.{ DateTime, DateTimeZone }
import io.useless.play.json.DateTimeJson._

import models.budget.PlannedTransaction
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class PlannedTransactionsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /plannedTransactions" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/plannedTransactions").get }
      response.status mustBe UNAUTHORIZED
    }

    "return only PlannedTransactions belonging to the authenticated user" in {
      TestService.deletePlannedTransactions()

      val includedAccount = TestService.createPlannedTransaction(
        accessToken = TestService.accessToken
      )

      val excludedAccount = TestService.createPlannedTransaction(
        accessToken = TestService.otherAccessToken
      )

      val response = await { authenticatedRequest("/plannedTransactions").get }
      val plannedTransactions = response.json.as[Seq[PlannedTransaction]]
      plannedTransactions.length mustBe 1
      plannedTransactions.head.guid mustBe includedAccount.guid
    }

  }

  "POST /plannedTransactions" must {

    lazy val transactionType = TestService.createTransactionType()
    lazy val account = TestService.createAccount()
    val minTimestamp = DateTime.now.plusDays(10).toDateTime(DateTimeZone.UTC)
    val maxTimestamp = DateTime.now.plusDays(15).toDateTime(DateTimeZone.UTC)

    lazy val json = Json.obj(
      "transactionTypeGuid" -> transactionType.guid,
      "accountGuid" -> account.guid,
      "minAmount" -> 100.0,
      "maxAmount" -> 200.0,
      "minTimestamp" -> minTimestamp,
      "maxTimestamp" -> maxTimestamp
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/plannedTransactions").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict if any required fields aren't specified" in {
      Seq("transactionTypeGuid", "accountGuid").foreach { field =>
        val response = await { authenticatedRequest("/plannedTransactions").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new Transaction if authorized and valid" in {
      val response = await { authenticatedRequest("/plannedTransactions").post(json) }
      response.status mustBe CREATED

      val plannedTransaction = response.json.as[PlannedTransaction]
      plannedTransaction.transactionTypeGuid mustBe transactionType.guid
      plannedTransaction.accountGuid mustBe account.guid
      plannedTransaction.minAmount mustBe Some(100.0)
      plannedTransaction.maxAmount mustBe Some(200.0)
      plannedTransaction.minTimestamp mustBe Some(minTimestamp)
      plannedTransaction.maxTimestamp mustBe Some(maxTimestamp)
    }

  }

}
