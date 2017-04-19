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
              "reps": 15,
              "movement": {
                "guid": "${pushUp.guid}"
              }
            }
          ]
        }
      """)

      val tasks = TaskResolver.resolveTasks(workout)
      tasks.length mustBe 4

      tasks(0).movement.guid mustBe pullUp.guid
      tasks(0).reps mustBe Some(10)

      tasks(1).movement.guid mustBe pushUp.guid
      tasks(1).reps mustBe Some(15)

      tasks(2).movement.guid mustBe pullUp.guid
      tasks(2).reps mustBe Some(10)

      tasks(3).movement.guid mustBe pushUp.guid
      tasks(3).reps mustBe Some(15)
    }

    "return AMRAP subtasks" ignore {
      testHelper.clearDb()
      val pullUp = testHelper.createMovement("Pull Up")
      val pushUp = testHelper.createMovement("Push Up")

      val workout = testHelper.buildWorkoutFromJson(s"""
        {
          "name": "Boring, Looped",
          "score": "reps",
          "tasks": [
            {
              "time" : {
                "unitOfMeasure": "sec",
                "value": 120
              },
              "tasks": [
                {
                  "reps": 10,
                  "movement": {
                    "guid": "${pullUp.guid}"
                  }
                },
                {
                  "reps": 15,
                  "movement": {
                    "guid": "${pushUp.guid}"
                  }
                }
              ]
            },
            {
              "time" : {
                "unitOfMeasure": "sec",
                "value": 360
              },
              "tasks": [
                {
                  "reps": 2,
                  "movement": {
                    "guid": "${pullUp.guid}"
                  }
                },
                {
                  "reps": 4,
                  "movement": {
                    "guid": "${pushUp.guid}"
                  }
                }
              ]
            }
          ]
        }
      """)

      val tasks = TaskResolver.resolveTasks(workout)

      tasks(0).movement.guid mustBe pullUp.guid
      tasks(0).reps mustBe Some(10)
    }

  }

}
