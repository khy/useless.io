package test.workouts.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._

import models.workouts._
import test.workouts._

class WorkoutSpec extends IntegrationSpec {

  "POST /workouts" must {

    "reject unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/workouts").post(Json.obj())
      }

      response.status mustBe UNAUTHORIZED
    }

    "reject a workout that does not have a task" in {
      val response = await {
        request("/workouts").post(Json.parse("""
          {
            "name": "Rest"
          }
        """))
      }

      response.status mustBe BAD_REQUEST
    }

    "reject a workout with an invalid movement GUID" in {
      val invalidMovementGuid = UUID.randomUUID
      val response = await {
        request("/workouts").post(Json.parse(s"""
          {
            "score": "workout.task.time",
            "task": {
              "while": "rep < 10",
              "movement": "$invalidMovementGuid"
            }
          }
        """))
      }

      response.status mustBe BAD_REQUEST
      val error = response.json.as[Seq[Errors]].head
      error.key mustBe Some("/task/movement")
      error.messages.head.key mustBe "unknown"
    }

    "create a valid workout" in {
      val pullUp = testHelper.createMovement("Pull Up")

      val response = await { request("/workouts").post(Json.parse(s"""
        {
          "score": "workout.task.time",
          "task": {
            "while": "rep < 10",
            "movement": "${pullUp.guid}"
          }
        }
      """)) }

      response.status mustBe CREATED
    }

  }

}
