package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.DateTime

import models.budget.{Account, AccountType}
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class AccountsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /accounts" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/accounts").get }
      response.status mustBe UNAUTHORIZED
    }

    "return only Accounts belonging to contexts that the authenticated user belongs to" in {
      TestService.deleteAccounts()

      val includedAccount1 = TestService.createAccount(
        contextGuid = TestService.myContext.guid,
        name = "My Account",
        accessToken = TestService.accessToken
      )

      val includedAccount2 = TestService.createAccount(
        contextGuid = TestService.sharedContext.guid,
        name = "My Account",
        accessToken = TestService.otherAccessToken
      )

      val excludedAccount = TestService.createAccount(
        contextGuid = TestService.otherContext.guid,
        name = "Another Account",
        accessToken = TestService.otherAccessToken
      )

      val response = await { authenticatedRequest("/accounts").get }
      val accountGuids = response.json.as[Seq[Account]].map(_.guid)
      accountGuids.length mustBe 2
      accountGuids must contain (includedAccount1.guid)
      accountGuids must contain (includedAccount2.guid)
      accountGuids must not contain (excludedAccount.guid)
    }

    "return Accounts with the appropriate balance (ignoring soft-deleted transactions)" in {
      TestService.deleteAccounts()
      val account1 = TestService.createAccount(initialBalance = 50.0)
      val account2 = TestService.createAccount(initialBalance = 25.0)

      TestService.createTransaction(accountGuid = account1.guid, amount = 100.0)
      TestService.createTransaction(accountGuid = account1.guid, amount = -25.0)
      val toDelete = TestService.createTransaction(accountGuid = account1.guid, amount = 75.0)
      TestService.softDeleteTransaction(toDelete.guid)

      TestService.createTransaction(accountGuid = account2.guid, amount = -75.0)
      TestService.createTransaction(accountGuid = account2.guid, amount = 50.0)

      val response = await { authenticatedRequest("/accounts").get }
      val accounts = response.json.as[Seq[Account]]
      val _account1 = accounts.find(_.guid == account1.guid).get
      _account1.balance mustBe 125.0
      val _account2 = accounts.find(_.guid == account2.guid).get
      _account2.balance mustBe 0.0
    }

    "return accounts limited to the specified context" in {
      TestService.deleteAccounts()

      val excludedAccount = TestService.createAccount(
        contextGuid = TestService.myContext.guid,
        name = "My Account",
        accessToken = TestService.accessToken
      )

      val includedAccount = TestService.createAccount(
        contextGuid = TestService.sharedContext.guid,
        name = "Shared Account",
        accessToken = TestService.otherAccessToken
      )

      val response = await {
        authenticatedRequest("/accounts").withQueryString(
          "context" -> TestService.sharedContext.guid.toString
        ).get
      }

      val accountGuids = response.json.as[Seq[Account]].map(_.guid)
      accountGuids must contain (includedAccount.guid)
      accountGuids must not contain (excludedAccount.guid)
    }

  }

  "POST /accounts" must {

    lazy val context = TestService.createContext()

    lazy val json = Json.obj(
      "contextGuid" -> context.guid,
      "accountType" -> AccountType.Checking.key,
      "name" -> "Shared Checking",
      "initialBalance" -> 100.75
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/accounts").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("accountType", "name", "initialBalance").foreach { field =>
        val response = await { authenticatedRequest("/accounts").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a 409 Conflict if name is an empty string" in {
      val _json = json ++ Json.obj("name" -> "")
      val response = await { authenticatedRequest("/accounts").post(_json) }
      response.status mustBe CONFLICT
    }

    "return a 409 if the name already exists in the context" in {
      val excludedAccount = TestService.createAccount(
        contextGuid = context.guid,
        name = "Shared Checking",
        accessToken = TestService.accessToken
      )

      val response1 = await { authenticatedRequest("/accounts").post(json) }
      response1.status mustBe CONFLICT

      val otherContext = TestService.createContext()
      val _json = json ++ Json.obj("contextGuid" -> otherContext.guid)
      val response2 = await { authenticatedRequest("/accounts").post(_json) }
      response2.status mustBe CREATED
    }

    "return a new Account if authorized and valid" in {
      TestService.deleteAccounts()
      val response = await { authenticatedRequest("/accounts").post(json) }
      response.status mustBe CREATED

      val account = response.json.as[Account]
      account.contextGuid mustBe context.guid
      account.accountType mustBe AccountType.Checking
      account.name mustBe "Shared Checking"
      account.initialBalance mustBe 100.75
    }

  }

}
