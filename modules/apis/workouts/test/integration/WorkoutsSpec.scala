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

class WorkoutsSpec extends IntegrationSpec {

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
      message.key mustBe "unknownWorkoutGuid"
      message.details("guid") mustBe badMovementGuid.toString
    }

    "create a workout with subtasks" in {
      val pullUp = testHelper.createMovement("Pull Up")

      val pushJerk = testHelper.createMovement(
        name = "Push Jerk",
        variables = Some(Seq(Variable(
          name = "Barbell Weight",
          dimension = Some(Dimension.Weight),
          measurement = None
        )))
      )

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
