package test.workouts.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.workouts._
import models.workouts.JsonImplicits._
import test.workouts._

class WorkoutsSpec extends IntegrationSpec {

  "POST /workouts" must {

    "reject unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/workouts").post(Json.obj())
      }

      response.status mustBe UNAUTHORIZED
    }

    "create a movement that has variables" in {
      val pullUpResponse = await {
        request("/movements").post(Json.parse("""
          {
            "name": "Pull Up"
          }
        """))
      }
      val pullUp = pullUpResponse.json.as[Movement]

      val pushJerkResponse = await {
        request("/movements").post(Json.parse("""
          {
            "name": "Push Jerk",
            "variables": [
              {
                "name": "Barbell Weight",
                "dimension": "weight"
              }
            ]
          }
        """))
      }
      val pushJerk = pushJerkResponse.json.as[Movement]

      val response = await { request("/workouts").post(Json.parse(s"""
        {
          "name": "Barbell Half Angie",
          "reps": 1,
          "score": "time",
          "tasks": [
            {
              "reps": 100,
              "movement": {
                "guid": "${pullUp.guid}"
              }
            },
            {
              "reps": 100,
              "movement": {
                "guid": "${pushJerk.guid}",
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
          ]
        }
      """)) }

      response.status mustBe CREATED

      val workout = response.json.as[Workout]
      workout.name mustBe Some("Barbell Half Angie")
      workout.reps mustBe Some(1)
      workout.score mustBe Some("time")

      val pullUpTask = workout.tasks.get(0)
      pullUpTask.reps mustBe Some(100)
      pullUpTask.movement.get.guid mustBe pullUp.guid

      val pushJerkTask = workout.tasks.get(1)
      pushJerkTask.reps mustBe Some(100)
      pushJerkTask.movement.get.guid mustBe pushJerk.guid

      val barbellWeightVar = pushJerkTask.movement.get.variables.get(0)
      barbellWeightVar.name mustBe "Barbell Weight"
      barbellWeightVar.measurement.get.unitOfMeasure mustBe UnitOfMeasure.Pound
      barbellWeightVar.measurement.get.value mustBe 95
    }

  }

}
