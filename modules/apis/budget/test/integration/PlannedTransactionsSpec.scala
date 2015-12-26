package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.LocalDate
import io.useless.play.json.DateTimeJson._
import io.useless.validation.Validation

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

      val includedPlannedTransaction = TestService.createPlannedTransaction(
        accessToken = TestService.accessToken
      )

      val excludedPlannedTransaction = TestService.createPlannedTransaction(
        accessToken = TestService.otherAccessToken
      )

      val response = await { authenticatedRequest("/plannedTransactions").get }
      val plannedTransactions = response.json.as[Seq[PlannedTransaction]]
      plannedTransactions.length mustBe 1
      plannedTransactions.head.guid mustBe includedPlannedTransaction.guid
    }

    "return the transaction GUID that the planned transaction has been associated with, if any" in {
      TestService.deletePlannedTransactions()
      val plannedTransaction = TestService.createPlannedTransaction()
      val transaction = TestService.createTransaction(plannedTransactionGuid = Some(plannedTransaction.guid))

      val response = await { authenticatedRequest("/plannedTransactions").get }
      val plannedTransactions = response.json.as[Seq[PlannedTransaction]]
      plannedTransactions.head.transactionGuid mustBe Some(transaction.guid)
    }

  }

  "POST /plannedTransactions" must {

    lazy val transactionType = TestService.createTransactionType()
    lazy val account = TestService.createAccount()
    val minDate = LocalDate.now.plusDays(10)
    val maxDate = LocalDate.now.plusDays(15)

    lazy val json = Json.obj(
      "transactionTypeGuid" -> transactionType.guid,
      "accountGuid" -> account.guid,
      "minAmount" -> 100.0,
      "maxAmount" -> 200.0,
      "minDate" -> minDate,
      "maxDate" -> maxDate
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
      plannedTransaction.minDate mustBe Some(minDate)
      plannedTransaction.maxDate mustBe Some(maxDate)
    }

  }

  "DELETE /plannedTransactions/:guid" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val plannedTransaction = TestService.createPlannedTransaction(accessToken = TestService.accessToken)
      val response = await { unauthentictedRequest(s"/plannedTransactions/${plannedTransaction.guid}").delete }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict if the specified GUID doesn't exist" in {
      val response = await { authenticatedRequest(s"/plannedTransactions/${UUID.randomUUID}").delete }
      response.status mustBe CONFLICT
      ((response.json \ "plannedTransactionGuid")(0) \ "key").as[String] mustBe "useless.error.unknownGuid"
    }

    "return a 409 Conflict if the specified GUID doesn't belong to the authenticated account" in {
      val plannedTransaction = TestService.createPlannedTransaction(accessToken = TestService.otherAccessToken)
      val response = await { authenticatedRequest(s"/plannedTransactions/${plannedTransaction.guid}").delete }
      response.status mustBe CONFLICT
      ((response.json \ "plannedTransactionGuid")(0) \ "key").as[String] mustBe "useless.error.unauthorized"
    }

    "return a 204 No Content if the specified GUID exists and belongs to the authenticated account" in {
      TestService.deletePlannedTransactions()
      val plannedTransaction = TestService.createPlannedTransaction(accessToken = TestService.accessToken)

      val deleteResponse = await { authenticatedRequest(s"/plannedTransactions/${plannedTransaction.guid}").delete }
      deleteResponse.status mustBe NO_CONTENT

      val indexResponse = await { authenticatedRequest("/plannedTransactions").get }
      val plannedTransactions = indexResponse.json.as[Seq[PlannedTransaction]]
      plannedTransactions.length mustBe 0
    }

  }

}
