package dsl.workouts

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.workouts._
import test.workouts._

class WorkoutValidatorSpec extends IntegrationSpec {

  import testHelper.core._

  "WorkoutValidator.validateWorkout" must {

    "accept the workout built by TestHelper" in {
      val workout = buildWorkout()
      WorkoutValidator.validateWorkout(workout) mustBe Nil
    }

    "reject a workout with a task with both a movement and tasks" in {
      val workout = buildWorkout(
        task = buildAbstractTask(
          movement = Some(UUID.randomUUID),
          tasks = Some(Seq(buildAbstractTask()))
        )
      )

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout).head
      jsPath mustBe JsPath \ "task"
      errors.head.message mustBe "movement-and-tasks-specified"
    }

    "reject a workout with a task with both tasks and a constraint" in {
      val workout = buildWorkout(
        task = buildAbstractTask(
          tasks = Some(Seq(buildAbstractTask())),
          constraints = Some(Seq(buildConstraint()))
        )
      )

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout).head
      jsPath mustBe JsPath \ "task"
      errors.head.message mustBe "tasks-and-constraints-specified"
    }

    "reject a workout with invalid sub-tasks" in {
      val workout = buildWorkout(
        task = buildAbstractTask(
          tasks = Some(Seq(
            buildAbstractTask(
              movement = Some(UUID.randomUUID),
              tasks = Some(Seq(buildAbstractTask()))
            )
          ))
        )
      )

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout).head
      jsPath mustBe (JsPath \ "task" \ "tasks")(0)
      errors.head.message mustBe "movement-and-tasks-specified"
    }

  }

}
