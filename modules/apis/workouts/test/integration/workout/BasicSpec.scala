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

class BasicSpec extends IntegrationSpec {

  "POST /workouts" must {

    "reject unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/workouts").post(Json.obj())
      }

      response.status mustBe UNAUTHORIZED
    }

    "reject a workout that references a non-existant movement" in {
      val badMovementGuid = UUID.randomUUID
      val response = await { request("/workouts").post(Json.parse(s"""
        {
          "name": "1 Rep Max",
          "reps": 1,
          "movement": {
            "guid": "${badMovementGuid}",
            "score": "Barbell Weight",
            "variables": [
              {
                "name": "Barbell Weight",
                "measurement": {
                  "unitOfMeasure": "lbs",
                  "value": 95
                }
              }
            ]
          }
        }
      """)) }

      response.status mustBe BAD_REQUEST

      val scalarErrors = response.json.as[Seq[Errors]].head
      val message = scalarErrors.messages.head
      message.key mustBe "unknownMovementGuid"
      message.details("guid") mustBe badMovementGuid.toString
    }

    "reject a workout that does not have either a movement or tasks" in {
      val response = await { request("/workouts").post(Json.parse(s"""
        {
          "name": "20 Reps",
          "reps": 20,
          "score": "time"
        }
      """)) }

      response.status mustBe BAD_REQUEST

      val scalarErrors = response.json.as[Seq[Errors]].head
      val message = scalarErrors.messages.head
      message.key mustBe "noTaskMovementOrSubTask"
    }

  }

}
