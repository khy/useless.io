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

  def buildCleanAndJerk() = testHelper.createMovementFromJson("""
    {
      "name": "Clean and Jerk",
      "variables": [
        {
          "name": "Barbell Weight",
          "dimension": "weight"
        }
      ]
    }
  """)

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

    "reject a workout that does not have the same score as its parent" in {
      testHelper.clearDb()
      val cleanAndJerk = buildCleanAndJerk()

      val grace = testHelper.createWorkoutFromJson(s"""
        {
          "name": "Pull Up Grace",
          "reps": 30,
          "score": "time",
          "movement": {
            "guid": "${cleanAndJerk.guid}"
          }
        }
      """)

      val response = await { request(s"/workouts/${grace.guid}/workouts").post(Json.parse(s"""
        {
          "name": "Women's Rx",
          "time": {
            "unitOfMeasure": "min",
            "value": 5
          },
          "score": "reps",
          "movement": {
            "guid": "${cleanAndJerk.guid}",
            "measurement": {
              "unitOfMeasure": "lb",
              "value": 95
            }
          }
        }
      """)) }

      response.status mustBe BAD_REQUEST
      val errors = response.json.as[Seq[Errors]].head
      errors.messages.head.key mustBe "scoreDoesNotMatchParent"
    }

    "reject a workout with tasks that do no match parent" ignore {
      testHelper.clearDb()
      val cleanAndJerk = buildCleanAndJerk()

      val grace = testHelper.createWorkoutFromJson(s"""
        {
          "name": "Grace",
          "reps": 1,
          "score": "time",
          "tasks": [
            {
              "reps": 30,
              "movement": {
                "guid": "${cleanAndJerk.guid}"
              }
            }
          ]
        }
      """)

      val snatch = testHelper.createMovementFromJson("""
        {
          "name": "Snatch",
          "variables": [
            {
              "name": "Barbell Weight",
              "dimension": "weight"
            }
          ]
        }
      """)

      val response = await { request("/workouts").post(Json.parse(s"""
        {
          "parentGuid": "${grace.guid}",
          "name": "Grace",
          "reps": 1,
          "score": "time",
          "tasks": [
            {
              "reps": 30,
              "movement": {
                "guid": "${snatch.guid}",
                "variables": [
                  {
                    "name": "Barbell Weight",
                    "measurement": {
                      "unitOfMeasure": "lb",
                      "value": 95
                    }
                  }
                ]
              }
            }
          ]
        }
      """)) }

      response.status mustBe BAD_REQUEST
    }

  }

}
