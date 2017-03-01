package dsl.workouts

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.scalatestplus.play.PlaySpec

import models.workouts._
import test.workouts._

class WorkoutDslSpec extends IntegrationSpec {

  "WorkoutDsl.mergeWorkouts" must {

    "return the specified workout if there's no ancestry" in {
      val workout = testHelper.buildWorkout()
      val _workout = WorkoutDsl.mergeWorkouts(workout, Nil)
      workout mustBe _workout
    }

    "take unspecified attribute from the parent, if available" in {
      val pullUp = testHelper.createMovement("Pull Up")

      val parent = testHelper.createWorkout(s"""
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

      val ancestry = await {
        applicationComponents.workoutsService.db2api(Seq(parent))
      }

      val child = testHelper.buildWorkout("""
        {
          "time": {
            "unitOfMeasure": "sec",
            "value": 600
          }
        }
      """)

      val workout = WorkoutDsl.mergeWorkouts(child, ancestry)
      workout.name mustBe Some("Parent")
      workout.time.map(_.value) mustBe Some(600)
    }

  }

  "take unspecified attribute from the grandparent, if available" in {
    val backSquat = testHelper.createMovementFromJson("""
      {
        "name": "Back Squat",
        "variables": [
          {
            "name": "Barbell Weight",
            "dimension": "weight"
          }
        ]
      }
    """)

    val grandparent = testHelper.createWorkout(s"""
      {
        "name": "Grandparent",
        "reps": 1,
        "score": "time",
        "tasks": [
          {
            "reps": 100,
            "movement": {
              "guid": "${backSquat.guid}"
            }
          }
        ]
      }
    """)

    val parent = testHelper.createWorkout(s"""
      {
        "name": "Men's Rx",
        "parentGuid": "${grandparent.guid}",
        "tasks": [
          {
            "reps": 100,
            "movement": {
              "guid": "${backSquat.guid}",
              "variables": [
                {
                  "name": "Barbell Weight",
                  "measurement": {
                    "unitOfMeasure": "lbs",
                    "value": 225
                  }
                }
              ]
            }
          }
        ]
      }
    """)

    val ancestry = await {
      applicationComponents.workoutsService.db2api(Seq(parent, grandparent))
    }

    val child = testHelper.buildWorkout("""
      {
        "time": {
          "unitOfMeasure": "sec",
          "value": 800
        }
      }
    """)

    val workout = WorkoutDsl.mergeWorkouts(child, ancestry)
    workout.score mustBe Some("time")
    workout.name mustBe Some("Men's Rx")
    workout.time.map(_.value) mustBe Some(800)
  }

}
