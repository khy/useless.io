package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.{ DateTime, DateTimeZone }
import io.useless.play.json.DateTimeJson._

import models.budget.Transaction
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class TransactionsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "POST /transactions" must {

    lazy val transactionGroup = TestService.createTransactionGroup()
    lazy val projection = TestService.createProjection()
    val timestamp = DateTime.now.toDateTime(DateTimeZone.UTC)

    lazy val json = Json.obj(
      "transactionGroupGuid" -> transactionGroup.guid,
      "amount" -> 100.0,
      "timestamp" -> timestamp,
      "projectionGuid" -> projection.guid
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactions").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("transactionGroupGuid", "amount", "timestamp").foreach { field =>
        val response = await { authenticatedRequest("/transactions").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new Transaction if authorized and valid" in {
      val response = await { authenticatedRequest("/transactions").post(json) }
      response.status mustBe CREATED

      val transaction = response.json.as[Transaction]
      transaction.transactionGroupGuid mustBe transactionGroup.guid
      transaction.amount mustBe 100.0
      transaction.timestamp mustBe timestamp
      transaction.projectionGuid mustBe Some(projection.guid)
    }

  }

}
