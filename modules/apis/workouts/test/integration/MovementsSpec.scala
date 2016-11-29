package test.workouts.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._

import models.workouts._
import models.workouts.JsonImplicits._
import test.workouts._

class MovementsSpec extends IntegrationSpec {

  "POST /movements" must {

    "reject unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/movements").post(Json.obj())
      }

      response.status mustBe UNAUTHORIZED
    }

    "reject a movement that does not have a name" in {
      val response = await {
        request("/movements").post(Json.parse("""
          {
            "variables": [
              {
                "name": "Barbell Weight",
                "dimension": "weight"
              }
            ]
          }
        """))
      }

      response.status mustBe BAD_REQUEST
    }

    "create a movement that does not have variables" in {
      val response = await {
        request("/movements").post(Json.parse("""
          {
            "name": "Push Up"
          }
        """))
      }

      response.status mustBe CREATED
      val movement = response.json.as[Movement]
      movement.name mustBe "Push Up"
    }

    "create a movement that has variables" in {
      val response = await {
        request("/movements").post(Json.parse("""
          {
            "name": "Wall Ball",
            "variables": [
              {
                "name": "Ball Weight",
                "dimension": "weight"
              },
              {
                "name": "Target Height",
                "dimension": "distance"
              }
            ]
          }
        """))
      }

      response.status mustBe CREATED
      val movement = response.json.as[Movement]
      movement.name mustBe "Wall Ball"
      movement.variables.get.length mustBe 2
      val ballWeight = movement.variables.get.find { _.name == "Ball Weight" }.get
      ballWeight.dimension mustBe Some(Dimension.Weight)
      val targetHeight = movement.variables.get.find { _.name == "Target Height" }.get
      targetHeight.dimension mustBe Some(Dimension.Distance)
    }

    "reject a movement that has an invalid dimesion" in {
      val response = await {
        request("/movements").post(Json.parse("""
          {
            "name": "Wall Ball",
            "variables": [
              {
                "name": "Ball Weight",
                "dimension": "luminosity"
              }
            ]
          }
        """))
      }

      response.status mustBe BAD_REQUEST
    }

    "reject a movement that has multiple variables with the same name" in {
      val response = await {
        request("/movements").post(Json.parse("""
          {
            "name": "Wall Ball",
            "variables": [
              {
                "name": "Ball Weight",
                "dimension": "weight"
              },
              {
                "name": "Ball Weight",
                "dimension": "distance"
              }
            ]
          }
        """))
      }

      response.status mustBe BAD_REQUEST
      val scalarErrors = response.json.as[Seq[Errors]].head
      val message = scalarErrors.messages.head
      message.key mustBe "duplicateVariableName"
      message.details("name") mustBe "Ball Weight"
    }

  }

}
