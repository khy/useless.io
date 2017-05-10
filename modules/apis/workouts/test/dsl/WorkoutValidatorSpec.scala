package dsl.workouts

import java.util.UUID
import play.api.libs.json.JsPath

import test.workouts.IntegrationSpec

class WorkoutValidatorSpec extends IntegrationSpec {

  import testHelper._

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
