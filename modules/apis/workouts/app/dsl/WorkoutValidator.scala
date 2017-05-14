package dsl.workouts

import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError

import models.workouts._

object WorkoutValidator {

  def validateWorkout(workout: core.Workout, referencedMovements: Seq[Movement]): Seq[(JsPath, Seq[ValidationError])] = {
    var errors = Seq.empty[(JsPath, Seq[ValidationError])]

    errors = errors ++ appendJsPath(JsPath \ "task", validateTask(workout.task, referencedMovements))

    workout.variables.foreach { variables =>
      var seenNames = Seq.empty[String]

      variables.zipWithIndex.foreach { case (variable, index) =>
        if (seenNames.contains(variable.name)) {
          errors = errors :+ ((JsPath \ "variables")(index) \ "name", Seq(ValidationError("duplicate")))
        } else {
          seenNames = seenNames :+ variable.name
        }
      }
    }

    errors
  }

  private def validateTask(task: core.AbstractTask, referencedMovements: Seq[Movement]): Seq[(JsPath, Seq[ValidationError])] = {
    var errors = Seq.empty[(JsPath, Seq[ValidationError])]

    task.movement.foreach { movement =>
      val refMovement = referencedMovements.find { _.guid == movement }

      if (refMovement.isEmpty) {
        errors = errors :+ (JsPath \ "movement", Seq(ValidationError("unknown")))
      }

      refMovement.foreach { refMovement =>
        task.constraints.foreach { constraints =>
          constraints.zipWithIndex.foreach { case (constraint, index) =>
            val variable = refMovement.variables.getOrElse(Nil).find { _.name == constraint.variable }

            if (variable.isEmpty) {
              errors = errors :+ ((JsPath \ "constraints")(index) \ "variable", Seq(ValidationError("unknown")))
            }
          }
        }
      }
    }

    if (task.movement.isDefined && task.tasks.isDefined) {
      errors = errors :+ (JsPath, Seq(ValidationError("movement-and-tasks-specified")))
    }

    if (task.constraints.isDefined && task.tasks.isDefined) {
      errors = errors :+ (JsPath, Seq(ValidationError("tasks-and-constraints-specified")))
    }

    task.tasks.foreach { tasks =>
      errors = errors ++ tasks.zipWithIndex.flatMap { case (task, index) =>
        appendJsPath((JsPath \ "tasks")(index), validateTask(task, referencedMovements))
      }
    }

    errors
  }

  private def appendJsPath(prefixJsPath: JsPath, errors: Seq[(JsPath, Seq[ValidationError])]) = {
    errors.map { case (jsPath, errors) => (prefixJsPath ++ jsPath, errors) }
  }

}
