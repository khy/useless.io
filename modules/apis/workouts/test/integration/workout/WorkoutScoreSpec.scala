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

class WorkoutScoreSpec extends IntegrationSpec {

  val movement = testHelper.createMovement()

  "POST /workouts" must {

    "reject a workout that has no score" in {
      val response = await { request("/workouts").post(Json.parse(s"""
        {
          "name": "1 Rep",
          "reps": 1,
          "movement": {
            "guid": "${movement.guid}",
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
      message.key mustBe "noScoreSpecified"
    }

    "reject a workout that has multiple scores" in {
      val response = await { request("/workouts").post(Json.parse(s"""
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
      message.key mustBe "multipleScoresSpecified"
    }

    "reject a workout that has a top-level score that is neither 'time' or 'reps'" in {
      val response = await { request("/workouts").post(Json.parse(s"""
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
      message.key mustBe "invalidTopLevelScore"
      message.details("score") mustBe "Barbell Weight"
    }

  }

}
