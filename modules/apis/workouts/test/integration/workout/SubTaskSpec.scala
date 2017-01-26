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

class SubTaskSpec extends IntegrationSpec {

  val movement = testHelper.createMovement()

  "POST /workouts" must {

    "create a workout with subtasks" in {
      val pullUp = testHelper.createMovement("Pull Up")
      val pushUp = testHelper.createMovement("Push Up")
      val sitUp = testHelper.createMovement("Sit Up")

      val response = await { request("/workouts").post(Json.parse(s"""
        {
          "name": "Sub",
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
              "reps": 2,
              "tasks": [
                {
                  "reps": 25,
                  "movement": {
                    "guid": "${pushUp.guid}"
                  }
                },
                {
                  "reps": 25,
                  "movement": {
                    "guid": "${sitUp.guid}"
                  }
                }
              ]
            }
          ]
        }
      """)) }

      response.status mustBe CREATED

      val workout = response.json.as[Workout]
      workout.name mustBe Some("Sub")
      workout.reps mustBe Some(Formula.Constant(1))
      workout.score mustBe Some("time")

      val pullUpTask = workout.tasks.get(0)
      pullUpTask.reps mustBe Some(Formula.Constant(100))
      pullUpTask.movement.get.guid mustBe pullUp.guid

      val compositeTask = workout.tasks.get(1)
      compositeTask.reps mustBe Some(Formula.Constant(2))

      val pushUpTask = compositeTask.tasks.get(0)
      pushUpTask.reps mustBe Some(Formula.Constant(25))
      pushUpTask.movement.get.guid mustBe pushUp.guid

      val sitUpTask = compositeTask.tasks.get(1)
      sitUpTask.reps mustBe Some(Formula.Constant(25))
      sitUpTask.movement.get.guid mustBe sitUp.guid
    }

  }

}
