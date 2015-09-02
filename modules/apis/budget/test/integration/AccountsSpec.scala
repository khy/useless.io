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

    "return only Accounts belonging to the authenticated user" in {
      TestService.deleteAccounts()

      val includedAccount = TestService.createAccount(
        name = "My Account",
        accessToken = TestService.accessToken
      )

      val excludedAccount = TestService.createAccount(
        name = "Another Account",
        accessToken = TestService.otherAccessToken
      )

      val response = await { authenticatedRequest("/accounts").get }
      val accounts = response.json.as[Seq[Account]]
      accounts.length mustBe 1
      accounts.head.guid mustBe includedAccount.guid
    }

  }

  "POST /accounts" must {

    val json = Json.obj(
      "accountType" -> AccountType.Checking.key,
      "name" -> "Shared Checking",
      "initialBalance" -> 100.75
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/accounts").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("accountType", "name").foreach { field =>
        val response = await { authenticatedRequest("/accounts").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new Account if authorized and valid" in {
      val response = await { authenticatedRequest("/accounts").post(json) }
      response.status mustBe CREATED

      val account = response.json.as[Account]
      account.accountType mustBe AccountType.Checking
      account.name mustBe "Shared Checking"
      account.initialBalance mustBe Some(100.75)
    }

  }

}
