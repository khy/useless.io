package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.budget.TransactionType
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

    "return 200 OK with internal transaction types, if so specified" in {
      val expense = TestService.getInternalTransactionType("Expense")
      TestService.createTransactionType("Rent", Some(expense.guid))

      val response = await {
        authenticatedRequest("/transactionTypes").
          withQueryString("internal" -> "true").get
      }
      response.status mustBe OK
      val transactionTypeNames = response.json.as[Seq[TransactionType]].map(_.name)
      transactionTypeNames must contain ("Income")
      transactionTypeNames must contain ("Expense")
      transactionTypeNames must not contain ("Rent")
    }

  }

  "POST /transactionTypes" must {

    lazy val account = TestService.createAccount(name = "Shared Checking")
    lazy val expense = TestService.getInternalTransactionType("Expense")

    lazy val json = Json.obj(
      "name" -> "Rent",
      "parentGuid" -> expense.guid,
      "accountGuid" -> account.guid
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
      transactionType.accountGuid mustBe Some(account.guid)
    }

  }

}
