package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.budget.TransactionClass
import models.budget.JsonImplicits._
import test.budget.integration.util.IntegrationHelper

class TransactionClassesSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /transactionClasses" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactionClasses").get }
      response.status mustBe UNAUTHORIZED
    }

    "return 200 OK with all of the support TransactionClasses" in {
      val response = await { authenticatedRequest("/transactionClasses").get }
      response.status mustBe OK
      val transactionClasses = response.json.as[Seq[TransactionClass]]
      transactionClasses must contain (TransactionClass.Credit)
      transactionClasses must contain (TransactionClass.Debit)
    }

  }

}
