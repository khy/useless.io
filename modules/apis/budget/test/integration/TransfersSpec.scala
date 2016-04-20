package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.LocalDate
import io.useless.play.json.DateTimeJson._

import models.budget.Transfer
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class TransfersSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "POST /transfers" must {

    lazy val context = TestService.createContext()
    lazy val fromAccount = TestService.createAccount(contextGuid = context.guid, name = "Checking")
    lazy val toAccount = TestService.createAccount(contextGuid = context.guid, name = "Savings")
    val date = LocalDate.now

    lazy val json = Json.obj(
      "fromAccountGuid" -> fromAccount.guid,
      "toAccountGuid" -> toAccount.guid,
      "amount" -> 100.0,
      "date" -> date
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transfers").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict if any required fields aren't specified" in {
      Seq("fromAccountGuid", "toAccountGuid", "amount", "date").foreach { field =>
        val response = await { authenticatedRequest("/transfers").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a 409 Conflict if the two accounts are not in the same context" in {
      val otherAccount = TestService.createAccount()
      val _json = json ++ Json.obj("toAccountGuid" -> otherAccount.guid)
      val response = await { authenticatedRequest("/transfers").post(_json) }
      response.status mustBe CONFLICT
    }

    "return a new Transfer if authorized and valid" in {
      val response = await { authenticatedRequest("/transfers").post(json) }
      response.status mustBe CREATED

      val transfer = response.json.as[Transfer]
      transfer.fromTransaction.amount mustBe -100
      transfer.toTransaction.amount mustBe 100

      val transferType = TestService.getSystemTransactionType("Transfer")
      transfer.fromTransaction.transactionTypeGuid mustBe transferType.guid
      transfer.toTransaction.transactionTypeGuid mustBe transferType.guid
    }

  }

}
