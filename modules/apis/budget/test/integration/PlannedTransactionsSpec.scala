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

    "return only PlannedTransactions for accounts in contexts that the authenticated user belongs to" in {
      TestService.deletePlannedTransactions()

      val account1 = TestService.createAccount(contextGuid = TestService.myContext.guid)
      val account2 = TestService.createAccount(contextGuid = TestService.sharedContext.guid)
      val account3 = TestService.createAccount(contextGuid = TestService.otherContext.guid)

      val includedPlannedTransaction1 = TestService.createPlannedTransaction(
        accountGuid = account1.guid,
        accessToken = TestService.accessToken
      )

      val includedPlannedTransaction2 = TestService.createPlannedTransaction(
        accountGuid = account2.guid,
        accessToken = TestService.otherAccessToken
      )

      val excludedPlannedTransaction = TestService.createPlannedTransaction(
        accountGuid = account3.guid,
        accessToken = TestService.otherAccessToken
      )

      val response = await { authenticatedRequest("/plannedTransactions").get }
      val plannedTransactionGuids = response.json.as[Seq[PlannedTransaction]].map(_.guid)
      plannedTransactionGuids.length mustBe 2
      plannedTransactionGuids must contain (includedPlannedTransaction1.guid)
      plannedTransactionGuids must contain (includedPlannedTransaction2.guid)
      plannedTransactionGuids must not contain (excludedPlannedTransaction.guid)
    }

    "return only the PlannedTransaction with the specified guid" in {
      TestService.deletePlannedTransactions()

      val includedPlannedTransaction = TestService.createPlannedTransaction()
      val excludedPlannedTransaction = TestService.createPlannedTransaction()

      val response = await {
        authenticatedRequest("/plannedTransactions").
          withQueryString("guid" -> includedPlannedTransaction.guid.toString).
          get
      }

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

    "return only PlannedTransactions belonging to the specified account" in {
      TestService.deletePlannedTransactions()

      val includedAccount = TestService.createAccount()
      val includedPlannedTransaction = TestService.createPlannedTransaction(
        accountGuid = includedAccount.guid
      )

      val excludedAccount = TestService.createAccount()
      val excludedPlannedTransaction = TestService.createPlannedTransaction(
        accountGuid = excludedAccount.guid
      )

      val response = await {
        authenticatedRequest("/plannedTransactions").
          withQueryString("account" -> includedAccount.guid.toString).
          get
      }

      val plannedTransactions = response.json.as[Seq[PlannedTransaction]]
      plannedTransactions.length mustBe 1
      plannedTransactions.head.guid mustBe includedPlannedTransaction.guid
    }

    "return only PlannedTransactions belonging to the specified context" in {
      TestService.deletePlannedTransactions()

      val account1 = TestService.createAccount(contextGuid = TestService.myContext.guid)
      val excludedPlannedTransaction = TestService.createPlannedTransaction(
        accountGuid = account1.guid
      )

      val account2 = TestService.createAccount(contextGuid = TestService.sharedContext.guid)
      val includedPlannedTransaction = TestService.createPlannedTransaction(
        accountGuid = account2.guid
      )

      val response = await {
        authenticatedRequest("/plannedTransactions").withQueryString(
          "context" -> TestService.sharedContext.guid.toString
        ).get
      }

      val plannedTransactionGuids = response.json.as[Seq[PlannedTransaction]].map(_.guid)
      plannedTransactionGuids must contain (includedPlannedTransaction.guid)
      plannedTransactionGuids must not contain (excludedPlannedTransaction.guid)
    }

    "return only PlannedTransactions for the specified transaction type" in {
      TestService.deletePlannedTransactions()
      val account = TestService.createAccount(contextGuid = TestService.myContext.guid)
      val includedTransactionType = TestService.createTransactionType(contextGuid = TestService.myContext.guid)
      val excludedTransactionType = TestService.createTransactionType(contextGuid = TestService.myContext.guid)

      val excludedPlannedTransaction = TestService.createPlannedTransaction(
        accountGuid = account.guid,
        transactionTypeGuid = excludedTransactionType.guid
      )

      val includedPlannedTransaction = TestService.createPlannedTransaction(
        accountGuid = account.guid,
        transactionTypeGuid = includedTransactionType.guid
      )

      val response = await {
        authenticatedRequest("/plannedTransactions").withQueryString(
          "transactionType" -> includedTransactionType.guid.toString
        ).get
      }

      val plannedTransactionGuids = response.json.as[Seq[PlannedTransaction]].map(_.guid)
      plannedTransactionGuids must contain (includedPlannedTransaction.guid)
      plannedTransactionGuids must not contain (excludedPlannedTransaction.guid)
    }

    "paginate the PlannedTransactions as specified" in {
      TestService.deletePlannedTransactions()

      val plannedTransaction1 = TestService.createPlannedTransaction()
      val plannedTransaction2 = TestService.createPlannedTransaction()
      val plannedTransaction3 = TestService.createPlannedTransaction()

      val response1 = await {
        authenticatedRequest("/plannedTransactions").
          withQueryString("p.limit" -> "2").
          get
      }

      val plannedTransactions1 = response1.json.as[Seq[PlannedTransaction]]
      plannedTransactions1.length mustBe 2
      plannedTransactions1.map(_.guid) must not contain plannedTransaction3.guid

      val response2 = await {
        authenticatedRequest("/plannedTransactions").
          withQueryString("p.page" -> "2", "p.limit" -> "2").
          get
      }

      val plannedTransactions2 = response2.json.as[Seq[PlannedTransaction]]
      plannedTransactions2.length mustBe 1
      plannedTransactions2.head.guid mustBe plannedTransaction3.guid
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
      "maxDate" -> maxDate,
      "name" -> "Test Planned Transaction"
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
      plannedTransaction.name mustBe Some("Test Planned Transaction")
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
