package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.budget.{TransactionGroup, TransactionType}
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class TransactionGroupsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "POST /transactionGroups" must {

    lazy val account = TestService.createAccount(name = "Shared Checking")

    lazy val json = Json.obj(
      "transactionType" -> TransactionType.Credit.key,
      "accountGuid" -> account.guid,
      "name" -> "Rent"
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactionGroups").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("transactionType", "accountGuid", "name").foreach { field =>
        val response = await { authenticatedRequest("/transactionGroups").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new TransactionGroup if authorized and valid" in {
      val response = await { authenticatedRequest("/transactionGroups").post(json) }
      response.status mustBe CREATED

      val transactionGroup = response.json.as[TransactionGroup]
      transactionGroup.transactionType mustBe TransactionType.Credit
      transactionGroup.accountGuid mustBe account.guid
      transactionGroup.name mustBe "Rent"
    }

  }

}
