package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.LocalDate
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

  "GET /transactions" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactions").get }
      response.status mustBe UNAUTHORIZED
    }

    "return only Transactions belonging to the authenticated user" in {
      TestService.deleteTransactions()

      val includedAccount = TestService.createTransaction(
        accessToken = TestService.accessToken
      )

      val excludedAccount = TestService.createTransaction(
        accessToken = TestService.otherAccessToken
      )

      val response = await { authenticatedRequest("/transactions").get }
      val transactions = response.json.as[Seq[Transaction]]
      transactions.length mustBe 1
      transactions.head.guid mustBe includedAccount.guid
    }

  }

  "POST /transactions" must {

    lazy val transactionType = TestService.createTransactionType()
    lazy val account = TestService.createAccount()
    val date = LocalDate.now
    lazy val plannedTransaction = TestService.createPlannedTransaction()

    lazy val json = Json.obj(
      "transactionTypeGuid" -> transactionType.guid,
      "accountGuid" -> account.guid,
      "amount" -> 100.0,
      "date" -> date,
      "name" -> "Test Transaction",
      "plannedTransactionGuid" -> plannedTransaction.guid
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactions").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("transactionTypeGuid", "accountGuid", "amount", "date").foreach { field =>
        val response = await { authenticatedRequest("/transactions").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new Transaction if authorized and valid" in {
      val response = await { authenticatedRequest("/transactions").post(json) }
      response.status mustBe CREATED

      val transaction = response.json.as[Transaction]
      transaction.transactionTypeGuid mustBe transactionType.guid
      transaction.amount mustBe 100.0
      transaction.date mustBe date
      transaction.name mustBe Some("Test Transaction")
      transaction.plannedTransactionGuid mustBe Some(plannedTransaction.guid)
    }

  }

  "DELETE /transactions" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val transaction = TestService.createTransaction(accessToken = TestService.accessToken)
      val response = await { unauthentictedRequest(s"/transactions/${transaction.guid}").delete }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict if the specified GUID doesn't exist" in {
      val response = await { authenticatedRequest(s"/transactions/${UUID.randomUUID}").delete }
      response.status mustBe CONFLICT
      ((response.json \ "transactionGuid")(0) \ "key").as[String] mustBe "useless.error.unknownGuid"
    }

    "return a 409 Conflict if the specified GUID doesn't belong to the authenticated account" in {
      val transaction = TestService.createTransaction(accessToken = TestService.otherAccessToken)
      val response = await { authenticatedRequest(s"/transactions/${transaction.guid}").delete }
      response.status mustBe CONFLICT
      ((response.json \ "transactionGuid")(0) \ "key").as[String] mustBe "useless.error.unauthorized"
    }

    "return a 204 No Content if the specified GUID exists and belongs to the authenticated account" in {
      TestService.deleteTransactions()
      val transaction = TestService.createTransaction(accessToken = TestService.accessToken)

      val deleteResponse = await { authenticatedRequest(s"/transactions/${transaction.guid}").delete }
      deleteResponse.status mustBe NO_CONTENT

      val indexResponse = await { authenticatedRequest("/transactions").get }
      val transactions = indexResponse.json.as[Seq[Transaction]]
      transactions.length mustBe 0
    }

  }

  "POST /transactions/:guid/adjustments" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val transaction = TestService.createTransaction()
      val json = Json.obj("amount" -> 200.0)

      val response = await { unauthentictedRequest(s"/transactions/${transaction.guid}/adjustments").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a new Transaction with the specified changes" in {
      val transaction = TestService.createTransaction(amount = 100.00)
      val json = Json.obj("amount" -> 95.0)

      val response = await { authenticatedRequest(s"/transactions/${transaction.guid}/adjustments").post(json) }
      response.status mustBe CREATED

      val _transaction = response.json.as[Transaction]
      _transaction.guid must not be transaction.guid
      _transaction.transactionTypeGuid mustBe transaction.transactionTypeGuid
      _transaction.accountGuid mustBe transaction.accountGuid
      _transaction.amount mustBe 95.0
      _transaction.date mustBe transaction.date
      _transaction.name mustBe transaction.name
      _transaction.adjustedTransactionGuid mustBe Some(transaction.guid)
    }

  }

}
