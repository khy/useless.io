package dsl.workouts

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.workouts._
import test.workouts._

class TaskResolverSpec extends IntegrationSpec {

  "TaskResolver.resolveTasks" must {

    "return 1 task for single movement workouts" in {
      val pullUp = testHelper.createMovement("Pull Up")
      val workout = testHelper.buildWorkoutFromJson(s"""
        {
          "name": "Pull Up Grace",
          "reps": 30,
          "score": "time",
          "movement": {
            "guid": "${pullUp.guid}"
          }
        }
      """)

      val tasks = TaskResolver.resolveTasks(workout)
      tasks.length mustBe 1

      val task = tasks.head
      task.movement.guid mustBe pullUp.guid
      task.reps mustBe Some(30)
      task.seconds mustBe None
    }

    "return subtasks in sequence" in {
      testHelper.clearDb()
      val pullUp = testHelper.createMovement("Pull Up")
      val pushUp = testHelper.createMovement("Push Up")

      val workout = testHelper.buildWorkoutFromJson(s"""
        {
          "name": "Boring",
          "reps": 1,
          "score": "time",
          "tasks": [
            {
              "reps": 10,
              "movement": {
                "guid": "${pullUp.guid}"
              }
            },
            {
              "reps": 10,
              "movement": {
                "guid": "${pushUp.guid}"
              }
            }
          ]
        }
      """)

      val tasks = TaskResolver.resolveTasks(workout)
      tasks.length mustBe 2
    }

    "return looped subtasks" in {
      testHelper.clearDb()
      val pullUp = testHelper.createMovement("Pull Up")
      val pushUp = testHelper.createMovement("Push Up")

      val workout = testHelper.buildWorkoutFromJson(s"""
        {
          "name": "Boring, Looped",
          "reps": 2,
          "score": "time",
          "tasks": [
            {
              "reps": 10,
              "movement": {
                "guid": "${pullUp.guid}"
              }
            },
            {
              "reps": 10,
              "movement": {
                "guid": "${pushUp.guid}"
              }
            }
          ]
        }
      """)

      val tasks = TaskResolver.resolveTasks(workout)
      tasks.length mustBe 4
    }

  }

}
