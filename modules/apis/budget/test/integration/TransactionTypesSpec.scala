package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.budget.{TransactionType, TransactionTypeOwnership}
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class TransactionTypesSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /transactionTypes" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactionTypes").get }
      response.status mustBe UNAUTHORIZED
    }

    "return 200 OK with system transaction types, if so specified" in {
      val expense = TestService.getSystemTransactionType("Expense")
      TestService.createTransactionType(
        name = "Rent",
        parentGuid = Some(expense.guid),
        ownership = TransactionTypeOwnership.User
      )

      val response = await {
        authenticatedRequest("/transactionTypes").
          withQueryString("ownership" -> TransactionTypeOwnership.System.key).get
      }
      response.status mustBe OK
      val transactionTypeNames = response.json.as[Seq[TransactionType]].map(_.name)
      transactionTypeNames must contain ("Income")
      transactionTypeNames must contain ("Expense")
      transactionTypeNames must not contain ("Rent")
    }

  }

  "POST /transactionTypes" must {

    lazy val expense = TestService.getSystemTransactionType("Expense")

    lazy val json = Json.obj(
      "name" -> "Rent",
      "parentGuid" -> expense.guid
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactionTypes").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("name").foreach { field =>
        val response = await { authenticatedRequest("/transactionTypes").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new TransactionType if authorized and valid" in {
      val response = await { authenticatedRequest("/transactionTypes").post(json) }
      response.status mustBe CREATED

      val transactionType = response.json.as[TransactionType]
      transactionType.name mustBe "Rent"
      transactionType.parentGuid mustBe Some(expense.guid)
    }

  }

}
