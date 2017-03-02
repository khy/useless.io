package test.workouts.integration.workout

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._

import models.workouts._
import models.workouts.JsonImplicits._
import test.workouts._

class ChildSpec extends IntegrationSpec {

  "POST /workouts" must {

    "create a workout with subtasks" in {
      val pullUp = testHelper.createMovement("Pull Up")

      val parent = testHelper.createWorkoutFromJson(s"""
        {
          "name": "Parent",
          "reps": 1,
          "score": "time",
          "tasks": [
            {
              "reps": 100,
              "movement": {
                "guid": "${pullUp.guid}"
              }
            }
          ]
        }
      """)

      val response = await { request("/workouts").post(Json.parse(s"""
        {
          "parentGuid": "${parent.guid}",
          "reps": 1,
          "score": "time",
          "time": {"value": 90, "unitOfMeasure": "sec"},
          "tasks": [
            {
              "reps": 100,
              "movement": {
                "guid": "${pullUp.guid}"
              }
            }
          ]
        }
      """)) }

      response.status mustBe CREATED
    }

  }

}
