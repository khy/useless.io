package test.workouts.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._

import models.workouts._
import test.workouts._

class MovementSpec extends IntegrationSpec {

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
      ballWeight.dimension mustBe core.Dimension.Weight
      val targetHeight = movement.variables.get.find { _.name == "Target Height" }.get
      targetHeight.dimension mustBe core.Dimension.Distance
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
      message.key mustBe "duplicate"
    }

  }

  "GET /movements" must {

    "accept unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/movements").get()
      }

      response.status mustBe OK
    }

    "return a paginated list of movements" in {
      testHelper.deleteMovements()
      val movement1 = testHelper.createMovement()
      val movement2 = testHelper.createMovement()
      val movement3 = testHelper.createMovement()

      val response = await {
        unauthenticatedRequest("/movements").get()
      }

      response.status mustBe OK
      val movements = response.json.as[Seq[Movement]]
      movements.length mustBe 3
    }

    "return movements filtered by name" in {
      testHelper.deleteMovements()
      val movement1 = testHelper.createMovement(name = "Alice")
      val movement2 = testHelper.createMovement(name = "Albert")
      val movement3 = testHelper.createMovement(name = "Anthony")
      val movement4 = testHelper.createMovement(name = "Alfred")

      val response = await {
        unauthenticatedRequest("/movements").withQueryString(
          "name" -> "Al",
          "p.order" -> "name"
        ).get()
      }

      response.status mustBe OK
      val movements = response.json.as[Seq[Movement]]
      movements.length mustBe 3
      movements.map(_.name) mustBe Seq("Albert", "Alfred", "Alice")
    }

  }

}
