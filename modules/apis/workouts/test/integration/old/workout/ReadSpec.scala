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

class ReadSpec extends IntegrationSpec {

  "GET /old/workouts" must {

    "accept unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/old/workouts").get()
      }

      response.status mustBe OK
    }

    "return all attributes of the workout" ignore {
      oldTestHelper.deleteWorkouts()
      val workout = oldTestHelper.createWorkout(
        score = Some("time"),
        time = Some(Measurement(UnitOfMeasure.Second, 50))
      )

      val response = await {
        unauthenticatedRequest("/old/workouts").withQueryString(
          "guid" -> workout.guid.toString
        ).get()
      }

      val _workout = response.json.as[Seq[Workout]].head
      _workout.score mustBe Some("time")
      _workout.time mustBe Some(Measurement(UnitOfMeasure.Second, 50))
    }

    "return a paginated list of workouts" ignore {
      oldTestHelper.deleteWorkouts()
      val workout1 = oldTestHelper.createWorkout()
      val workout2 = oldTestHelper.createWorkout()
      val workout3 = oldTestHelper.createWorkout()

      val response = await {
        unauthenticatedRequest("/old/workouts").get()
      }

      response.status mustBe OK
      val workouts = response.json.as[Seq[Workout]]
      workouts.length mustBe 3
    }

    "return workouts filtered by guid" ignore {
      oldTestHelper.deleteWorkouts()
      val workout1 = oldTestHelper.createWorkout()
      val workout2 = oldTestHelper.createWorkout()

      val response = await {
        unauthenticatedRequest("/old/workouts").withQueryString(
          "guid" -> workout1.guid.toString
        ).get()
      }

      response.status mustBe OK
      val movements = response.json.as[Seq[Workout]]
      movements.length mustBe 1
      movements.head.guid mustBe workout1.guid
    }

    "return workouts without parents" ignore {
      oldTestHelper.deleteWorkouts()
      val workout1 = oldTestHelper.createWorkout()
      val workout2 = oldTestHelper.createWorkout(
        parentGuid = Some(workout1.guid)
      )

      val response1 = await {
        unauthenticatedRequest("/old/workouts").withQueryString(
          "child" -> "false"
        ).get()
      }

      val movements1 = response1.json.as[Seq[Workout]]
      movements1.length mustBe 1
      movements1.head.guid mustBe workout1.guid

      val response2 = await {
        unauthenticatedRequest("/old/workouts").withQueryString(
          "child" -> "true"
        ).get()
      }

      val movements2 = response2.json.as[Seq[Workout]]
      movements2.length mustBe 1
      movements2.head.guid mustBe workout2.guid
    }

    "return child workouts for the specified parent" ignore {
      oldTestHelper.deleteWorkouts()
      val workout1 = oldTestHelper.createWorkout()
      val workout2 = oldTestHelper.createWorkout(
        parentGuid = Some(workout1.guid)
      )

      val response = await {
        unauthenticatedRequest("/old/workouts").withQueryString(
          "parentGuid" -> workout1.guid.toString
        ).get()
      }

      val movements = response.json.as[Seq[Workout]]
      movements.length mustBe 1
      movements.head.guid mustBe workout2.guid
    }

  }

}
