package dsl.workouts

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.workouts._
import test.workouts._

class WorkoutValidatorSpec extends IntegrationSpec {

  "WorkoutValidator.validateWorkout" must {

    "accept the workout built by TestHelper" in {
      val workout = testHelper.core.buildWorkout()
      WorkoutValidator.validateWorkout(workout) mustBe Nil
    }

  }

}
