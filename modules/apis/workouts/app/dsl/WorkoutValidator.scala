package dsl.workouts

import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError

import models.workouts.core._

object WorkoutValidator {

  def validateWorkout(workout: Workout): Seq[(JsPath, Seq[ValidationError])] = {
    appendJsPath(JsPath \ "task", validateTask(workout.task))
  }

  private def validateTask(task: AbstractTask): Seq[(JsPath, Seq[ValidationError])] = {
    var errors = Seq.empty[(JsPath, Seq[ValidationError])]

    if (task.movement.isDefined && task.tasks.isDefined) {
      errors = errors :+ (JsPath, Seq(ValidationError("movement-and-tasks-specified")))
    }

    if (task.constraints.isDefined && task.tasks.isDefined) {
      errors = errors :+ (JsPath, Seq(ValidationError("tasks-and-constraints-specified")))
    }

    task.tasks.foreach { tasks =>
      errors = errors ++ tasks.zipWithIndex.flatMap { case (task, index) =>
        appendJsPath((JsPath \ "tasks")(index), validateTask(task))
      }
    }

    errors
  }

  private def appendJsPath(prefixJsPath: JsPath, errors: Seq[(JsPath, Seq[ValidationError])]) = {
    errors.map { case (jsPath, errors) => (prefixJsPath ++ jsPath, errors) }
  }

}
