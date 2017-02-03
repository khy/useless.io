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

    "return workouts filtered by guid" in {
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

    "return workouts without parents" in {
      testHelper.deleteWorkouts()
      val workout1 = testHelper.createWorkout()
      val workout2 = testHelper.createWorkout(
        parentGuid = Some(workout1.guid),
        score = None
      )

      val response1 = await {
        unauthenticatedRequest("/workouts").withQueryString(
          "child" -> "false"
        ).get()
      }

      val movements1 = response1.json.as[Seq[Workout]]
      movements1.length mustBe 1
      movements1.head.guid mustBe workout1.guid

      val response2 = await {
        unauthenticatedRequest("/workouts").withQueryString(
          "child" -> "true"
        ).get()
      }

      val movements2 = response2.json.as[Seq[Workout]]
      movements2.length mustBe 1
      movements2.head.guid mustBe workout2.guid
    }

    "return child workouts for the specified parent" in {
      testHelper.deleteWorkouts()
      val workout1 = testHelper.createWorkout()
      val workout2 = testHelper.createWorkout(
        parentGuid = Some(workout1.guid),
        score = None
      )

      val response = await {
        unauthenticatedRequest("/workouts").withQueryString(
          "parentGuid" -> workout1.guid.toString
        ).get()
      }

      val movements = response.json.as[Seq[Workout]]
      movements.length mustBe 1
      movements.head.guid mustBe workout2.guid
    }

  }

}
