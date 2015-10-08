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
    val timestamp = DateTime.now.toDateTime(DateTimeZone.UTC)

    lazy val json = Json.obj(
      "transactionTypeGuid" -> transactionType.guid,
      "accountGuid" -> account.guid,
      "amount" -> 100.0,
      "timestamp" -> timestamp
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactions").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("transactionTypeGuid", "accountGuid", "amount", "timestamp").foreach { field =>
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
      transaction.timestamp mustBe timestamp
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
      _transaction.timestamp mustBe transaction.timestamp.toDateTime(DateTimeZone.UTC)
      _transaction.adjustedTransactionGuid mustBe Some(transaction.guid)
    }

  }

}
