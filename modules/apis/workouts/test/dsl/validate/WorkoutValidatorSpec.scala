package dsl.workouts.validate

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.json.JsPath

import db.workouts.MovementRecord
import dsl.workouts.compile._
import models.workouts._
import models.workouts.core
import test.workouts.IntegrationSpec

class WorkoutValidatorSpec extends IntegrationSpec {

  import testHelper._

  def db2api(record: MovementRecord): Movement = {
    await { applicationComponents.movementsService.db2api(Seq(record)) }.head
  }

  "WorkoutValidator.validateWorkout" must {

    "accept the workout built by TestHelper" in {
      val workout = buildWorkout()
      WorkoutValidator.validateWorkout(workout, Nil) mustBe Nil
    }

    "reject a workout with duplicate variable names" in {
      val movement = db2api(createMovement())

      val workout = buildWorkout(
        variables = Some(Seq(
          core.FreeVariable(name = "Body Weight", dimension = core.Dimension.Weight),
          core.FreeVariable(name = "Body Weight", dimension = core.Dimension.Weight)
        )),
        task = buildAbstractTask(
          movement = Some(movement.guid)
        )
      )

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout, Seq(movement)).head
      jsPath mustBe (JsPath \ "variables")(1) \ "name"
      errors.head.message mustBe "duplicate"
    }

    "reject a workout with a task with both a movement and tasks" in {
      val movement = db2api(createMovement())

      val workout = buildWorkout(
        task = buildAbstractTask(
          movement = Some(movement.guid),
          tasks = Some(Seq(buildAbstractTask()))
        )
      )

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout, Seq(movement)).head
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

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout, Nil).head
      jsPath mustBe JsPath \ "task"
      errors.head.message mustBe "tasks-and-constraints-specified"
    }

    "reject a workout if any movements are not in referencedMovements" in {
      val workout = buildWorkout(
        task = buildAbstractTask(
          movement = Some(UUID.randomUUID)
        )
      )

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout, Nil).head
      jsPath mustBe JsPath \ "task" \ "movement"
      errors.head.message mustBe "unknown"
    }

    "reject a workout with a task whose variables do not match the movements variables" in {
      val movement = db2api(createMovement(
        name = "Clean",
        variables = Some(Seq(
          core.FreeVariable(name = "Barbell Weight", dimension = core.Dimension.Weight)
        ))
      ))

      val workout = buildWorkout(
        task = buildAbstractTask(
          movement = Some(movement.guid),
          constraints = Some(Seq(buildConstraint(
            variable = "Box Height",
            value = core.Code("30", ArithmeticCompiler.compile("30").right.get)
          )))
        )
      )

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout, Seq(movement)).head
      jsPath mustBe (JsPath \ "task" \ "constraints")(0) \ "variable"
      errors.head.message mustBe "unknown"
    }

    "reject a workout with invalid sub-tasks" in {
      val movement = db2api(createMovement())

      val workout = buildWorkout(
        task = buildAbstractTask(
          tasks = Some(Seq(
            buildAbstractTask(
              movement = Some(movement.guid),
              tasks = Some(Seq(buildAbstractTask()))
            )
          ))
        )
      )

      val (jsPath, errors) = WorkoutValidator.validateWorkout(workout, Seq(movement)).head
      jsPath mustBe (JsPath \ "task" \ "tasks")(0)
      errors.head.message mustBe "movement-and-tasks-specified"
    }

  }

}
