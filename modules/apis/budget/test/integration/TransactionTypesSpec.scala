package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.budget.{TransactionType, TransactionClass}
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class TransactionTypesSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "POST /transactionTypes" must {

    lazy val account = TestService.createAccount(name = "Shared Checking")

    lazy val json = Json.obj(
      "transactionClass" -> TransactionClass.Credit.key,
      "accountGuid" -> account.guid,
      "name" -> "Rent"
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactionTypes").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("transactionClass", "accountGuid", "name").foreach { field =>
        val response = await { authenticatedRequest("/transactionTypes").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new TransactionType if authorized and valid" in {
      val response = await { authenticatedRequest("/transactionTypes").post(json) }
      response.status mustBe CREATED

      val transactionType = response.json.as[TransactionType]
      transactionType.transactionClass mustBe TransactionClass.Credit
      transactionType.accountGuid mustBe account.guid
      transactionType.name mustBe "Rent"
    }

  }

}