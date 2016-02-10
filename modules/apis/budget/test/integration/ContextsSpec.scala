package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.budget.Context
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class ContextsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "POST /contexts" must {

    lazy val json = Json.obj(
      "name" -> "Default",
      "userGuids" -> Seq("00000000-1111-1111-1111-111111111111")
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/contexts").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("name").foreach { field =>
        val response = await { authenticatedRequest("/contexts").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new Transfer if authorized and valid" in {
      val response = await { authenticatedRequest("/contexts").post(json) }
      response.status mustBe CREATED

      val context = response.json.as[Context]
      context.name mustBe "Default"
      context.users.map(_.guid.toString) mustBe Seq("00000000-1111-1111-1111-111111111111")
    }

  }

}
