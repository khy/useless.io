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

  "GET /contexts" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/contexts").get }
      response.status mustBe UNAUTHORIZED
    }

    "return only contexts associated with the authenticated account" in {
      val includedContext1 = TestService.createContext(
        userGuids = Seq(TestService.accessToken.resourceOwner.guid)
      )

      val includedContext2 = TestService.createContext(
        userGuids = Seq(
          TestService.accessToken.resourceOwner.guid,
          TestService.otherAccessToken.resourceOwner.guid
        )
      )

      val excludedContext = TestService.createContext(
        userGuids = Seq(TestService.otherAccessToken.resourceOwner.guid)
      )

      val response = await { authenticatedRequest("/contexts").get }
      response.status mustBe OK

      val contextGuids = response.json.as[Seq[Context]].map(_.guid)
      contextGuids must contain (includedContext1.guid)
      contextGuids must contain (includedContext2.guid)
      contextGuids must not contain (excludedContext.guid)
    }

  }

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
