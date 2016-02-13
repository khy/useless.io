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

    "return only Transactions for accounts in contexts that the authenticated user belongs to" in {
      TestService.deleteTransactions()

      val account1 = TestService.createAccount(contextGuid = TestService.myContext.guid)
      val account2 = TestService.createAccount(contextGuid = TestService.sharedContext.guid)
      val account3 = TestService.createAccount(contextGuid = TestService.otherContext.guid)

      val includedTransaction1 = TestService.createTransaction(
        accountGuid = account1.guid,
        accessToken = TestService.accessToken
      )

      val includedTransaction2 = TestService.createTransaction(
        accountGuid = account2.guid,
        accessToken = TestService.otherAccessToken
      )

      val excludedTransaction = TestService.createTransaction(
        accountGuid = account3.guid,
        accessToken = TestService.otherAccessToken
      )

      val response = await { authenticatedRequest("/transactions").get }
      val transactionGuids = response.json.as[Seq[Transaction]].map(_.guid)
      transactionGuids.length mustBe 2
      transactionGuids must contain (includedTransaction1.guid)
      transactionGuids must contain (includedTransaction2.guid)
      transactionGuids must not contain (excludedTransaction.guid)
    }

    "return only Transactions belonging to the specified account" in {
      TestService.deleteTransactions()

      val includedAccount = TestService.createAccount()
      val includedTransaction = TestService.createTransaction(
        accountGuid = includedAccount.guid
      )

      val excludedAccount = TestService.createAccount()
      val excludedTransaction = TestService.createTransaction(
        accountGuid = excludedAccount.guid
      )

      val response = await {
        authenticatedRequest("/transactions").
          withQueryString("accountGuid" -> includedAccount.guid.toString).
          get
      }

      val transactions = response.json.as[Seq[Transaction]]
      transactions.length mustBe 1
      transactions.head.guid mustBe includedTransaction.guid
    }

    "paginate the Transactions as specified" in {
      TestService.deleteTransactions()

      val transaction1 = TestService.createTransaction()
      val transaction2 = TestService.createTransaction()
      val transaction3 = TestService.createTransaction()

      val response1 = await {
        authenticatedRequest("/transactions").
          withQueryString("p.limit" -> "2").
          get
      }

      val transactions1 = response1.json.as[Seq[Transaction]]
      transactions1.length mustBe 2
      transactions1.map(_.guid) must not contain transaction3.guid

      val response2 = await {
        authenticatedRequest("/transactions").
          withQueryString("p.page" -> "2", "p.limit" -> "2").
          get
      }

      val transactions2 = response2.json.as[Seq[Transaction]]
      transactions2.length mustBe 1
      transactions2.head.guid mustBe transaction3.guid
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
