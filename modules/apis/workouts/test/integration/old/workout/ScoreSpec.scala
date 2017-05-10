package test.workouts.integration.old.workout

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._

import models.workouts.old._
import models.workouts.old.JsonImplicits._
import test.workouts._

class ScoreSpec extends IntegrationSpec {

  val movement = testHelper.createMovement()

  "POST /old/workouts" must {

    "reject a workout that has no score, and no parent" in {
      val response = await { request("/old/workouts").post(Json.parse(s"""
        {
          "name": "1 Rep",
          "reps": 1,
          "movement": {
            "guid": "${movement.guid}",
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
      """)) }

      response.status mustBe BAD_REQUEST
      val scalarErrors = response.json.as[Seq[Errors]].head
      val message = scalarErrors.messages.head
      message.key mustBe "noScoreSpecified"
    }

    "reject a workout that has multiple scores" in {
      val response = await { request("/old/workouts").post(Json.parse(s"""
        {
          "name": "1 Rep",
          "score": "time",
          "reps": 1,
          "movement": {
            "guid": "${movement.guid}",
            "score": "Barbell Weight",
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
      """)) }

      response.status mustBe BAD_REQUEST
      val scalarErrors = response.json.as[Seq[Errors]].head
      val message = scalarErrors.messages.head
      message.key mustBe "multipleScoresSpecified"
    }

    "reject a workout that has a top-level score that is neither 'time' or 'reps'" in {
      val response = await { request("/old/workouts").post(Json.parse(s"""
        {
          "name": "1 Rep",
          "score": "Barbell Weight",
          "reps": 1,
          "movement": {
            "guid": "${movement.guid}",
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
      """)) }

      response.status mustBe BAD_REQUEST
      val scalarErrors = response.json.as[Seq[Errors]].head
      val message = scalarErrors.messages.head
      message.key mustBe "invalidTopLevelScore"
      message.details("score") mustBe "Barbell Weight"
    }

    "accept a workout that has no score, but has a parent" in {
      val parentResponse = await { request("/old/workouts").post(Json.parse(s"""
        {
          "name": "100 Reps",
          "score": "time",
          "reps": 100,
          "movement": {
            "guid": "${movement.guid}"
          }
        }
      """)) }
      val parent = parentResponse.json.as[Workout]

      val childResponse = await { request("/old/workouts").post(Json.parse(s"""
        {
          "parentGuid": "${parent.guid}",
          "score": "time",
          "movement": {
            "guid": "${movement.guid}",
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
      """)) }

      childResponse.status mustBe CREATED
    }

    "reject a workout that has a movement score that doesn't reference a free " +
    "variable in either the referenced movement or is inline in the workout" in {
      val response = await { request("/old/workouts").post(Json.parse(s"""
        {
          "name": "1 Rep",
          "reps": 1,
          "movement": {
            "guid": "${movement.guid}",
            "score": "Target Height"
          }
        }
      """)) }

      response.status mustBe BAD_REQUEST
      val scalarErrors = response.json.as[Seq[Errors]].head
      val message = scalarErrors.messages.head
      message.key mustBe "unknownMovementScore"
      message.details("score") mustBe "Target Height"
    }

  }

}
