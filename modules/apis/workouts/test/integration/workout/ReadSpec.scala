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

class ReadSpec extends IntegrationSpec {

  "GET /workouts" must {

    "accept unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/workouts").get()
      }

      response.status mustBe OK
    }

    "return a paginated list of workouts" in {
      testHelper.deleteWorkouts()
      val workout1 = testHelper.createWorkout()
      val workout2 = testHelper.createWorkout()
      val workout3 = testHelper.createWorkout()

      val response = await {
        unauthenticatedRequest("/workouts").get()
      }

      response.status mustBe OK
      val workouts = response.json.as[Seq[Workout]]
      workouts.length mustBe 3
    }

    "return movements filtered by guid" in {
      testHelper.deleteWorkouts()
      val workout1 = testHelper.createWorkout()
      val workout2 = testHelper.createWorkout()

      val response = await {
        unauthenticatedRequest("/workouts").withQueryString(
          "guid" -> workout1.guid.toString
        ).get()
      }

      response.status mustBe OK
      val movements = response.json.as[Seq[Workout]]
      movements.length mustBe 1
      movements.head.guid mustBe workout1.guid
    }

  }

}
