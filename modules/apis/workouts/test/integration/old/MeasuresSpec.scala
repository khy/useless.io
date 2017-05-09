package test.workouts.integration.old

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.workouts.old._
import test.workouts._

class StaticSpec extends IntegrationSpec {

  implicit val df = Json.format[Dimension]
  implicit val umf = Json.format[UnitOfMeasure]

  "GET /old/dimensions" must {

    "accept unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/old/dimensions").get()
      }

      response.status mustBe OK
    }

    "return all dimensions" in {
      val response = await {
        unauthenticatedRequest("/old/dimensions").get()
      }

      val dimensions = response.json.as[Seq[JsObject]]
      dimensions.length mustBe 4
    }

  }

  "GET /old/unitsOfMeasure" must {

    "accept unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/old/unitsOfMeasure").get()
      }

      response.status mustBe OK
    }

    "return all dimensions" in {
      val response = await {
        unauthenticatedRequest("/old/unitsOfMeasure").get()
      }

      val unitsOfMeasure = response.json.as[Seq[JsObject]]
      unitsOfMeasure.length mustBe 9
    }

  }

}
