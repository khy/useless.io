package test.workouts.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WS
import play.api.libs.json._
import org.scalatest._
import org.scalatestplus.play._
import io.useless.accesstoken.AccessToken
import io.useless.http.LinkHeader

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
        request("/movements").post(Json.obj(
          "variables" -> Json.arr(
            Json.obj(
              "name" -> "Barbell Weight",
              "dimension" -> "weight"
            )
          )
        ))
      }

      response.status mustBe BAD_REQUEST
    }

    "create a movement that does not have variables" in {
      val response = await {
        request("/movements").post(Json.obj(
          "name" -> "Push Up"
        ))
      }

      response.status mustBe CREATED
      val movement = response.json.as[Movement]
      movement.name mustBe "Push Up"
    }

    "create a movement that has variables" in {
      val response = await {
        request("/movements").post(Json.obj(
          "name" -> "Wall Ball",
          "variables" -> Json.arr(
            Json.obj(
              "name" -> "Ball Weight",
              "dimension" -> "weight"
            ),
            Json.obj(
              "name" -> "Target Height",
              "dimension" -> "distance"
            )
          )
        ))
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
        request("/movements").post(Json.obj(
          "name" -> "Wall Ball",
          "variables" -> Json.arr(
            Json.obj(
              "name" -> "Ball Weight",
              "dimension" -> "luminosity"
            )
          )
        ))
      }

      response.status mustBe BAD_REQUEST
    }

  }

}
