package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.budget.AccountType
import models.budget.JsonImplicits._
import test.budget.integration.util.IntegrationHelper

class AccountTypesSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /accountTypes" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/accountTypes").get }
      response.status mustBe UNAUTHORIZED
    }

    "return 200 OK with all of the support TransactionClasses" in {
      val response = await { authenticatedRequest("/accountTypes").get }
      response.status mustBe OK
      val accountTypes = response.json.as[Seq[AccountType]]
      accountTypes must contain (AccountType.Credit)
      accountTypes must contain (AccountType.Checking)
      accountTypes must contain (AccountType.Savings)
    }

  }

}
