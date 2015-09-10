package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.budget.Projection
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class ProjectionsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /projections" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/projections").get }
      response.status mustBe UNAUTHORIZED
    }

    "return only Accounts belonging to the authenticated user" in {
      TestService.deleteProjections()

      val includedProjection = TestService.createProjection(
        name = "My Projection",
        accessToken = TestService.accessToken
      )

      val excludedProjection = TestService.createProjection(
        name = "Another Projection",
        accessToken = TestService.otherAccessToken
      )

      val response = await { authenticatedRequest("/projections").get }
      val projections = response.json.as[Seq[Projection]]
      projections.length mustBe 1
      projections.head.guid mustBe includedProjection.guid
    }

  }

  "POST /projections" must {

    val json = Json.obj(
      "name" -> "My Projection"
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/projections").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("name").foreach { field =>
        val response = await { authenticatedRequest("/projections").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a new Account if authorized and valid" in {
      val response = await { authenticatedRequest("/projections").post(json) }
      response.status mustBe CREATED

      val account = response.json.as[Projection]
      account.name mustBe "My Projection"
    }

  }

}
