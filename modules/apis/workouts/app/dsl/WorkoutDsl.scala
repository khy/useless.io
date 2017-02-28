package dsl.workouts

import io.useless.Message
import io.useless.validation._

import models.workouts._

object WorkoutDsl {

  def getSubTasks(workout: core.Workout) = {
    def _getSubTasks(subTasks: Seq[core.SubTask]): Seq[core.SubTask] = {
      if (!subTasks.isEmpty) {
        subTasks ++ _getSubTasks(subTasks.flatMap(_.tasks.getOrElse(Nil)))
      } else {
        Nil
      }
    }

    _getSubTasks(workout.tasks.getOrElse(Nil))
  }

  def mergeWorkouts(workout: core.Workout, ancestry: Seq[Workout]): core.Workout = {
    workout
  }

  def validateWorkout(workout: core.Workout, referencedMovements: Seq[Movement]): Seq[Errors] = {
    var errors = Seq.empty[Errors]

    val subTasks = getSubTasks(workout)
    val taskMovements = workout.movement.toSeq ++ subTasks.flatMap(_.movement)
    val scores = workout.score.toSeq ++ taskMovements.flatMap(_.score)

    // A workout _must_ have a score, but this probably need to be tweaked.
    if (scores.size == 0) {
      errors = errors :+ Errors.scalar(Seq(Message(key = "noScoreSpecified")))
    } else if (scores.size > 1) {
      errors = errors :+ Errors.scalar(Seq(Message(key = "multipleScoresSpecified")))
    }

    // A workout and all of its subtasks must have either a movement or at least one task
    if (workout.movement.isEmpty && workout.tasks.map(_.isEmpty).getOrElse(true)) {
      errors = errors :+ Errors.scalar(Seq(Message(key = "noTaskMovementOrSubTask")))
    }

    val emptySubTasks = subTasks.filter { subTask =>
      subTask.movement.isEmpty && subTask.tasks.map(_.isEmpty).getOrElse(true)
    }

    if (!emptySubTasks.isEmpty) {
      errors = errors :+ Errors.scalar(Seq(Message(key = "noTaskMovementOrSubTask")))
    }

    def taskMovementErrors(taskMovement: core.TaskMovement): Option[Errors] = {
      referencedMovements.find(_.guid == taskMovement.guid).map { referencedMovement =>
        // If a task movement has a score, it must reference a free variable
        // either in the task movement itself, or in the referenced movement.
        taskMovement.score.flatMap { score =>
          val freeVariableNames = (taskMovement.variables ++ referencedMovement.variables).flatten.filter { variable =>
            variable.dimension.isDefined
          }.map(_.name).toSeq

          if (!freeVariableNames.contains(score)) {
            Some(Errors.scalar(Seq(Message(
              key = "unknownMovementScore",
              details =
                "guid" -> taskMovement.guid.toString,
                "score" -> score
            ))))
          } else None
        }
      }.getOrElse {
        // The task movement's GUID must reference an actual movement.
        Some(Errors.scalar(Seq(Message(
          key = "unknownMovementGuid",
          details = "guid" -> taskMovement.guid.toString
        ))))
      }
    }

    errors = errors ++ taskMovements.flatMap(taskMovementErrors)

    // If there's a top-level score, it must be either 'time' or 'reps'.
    workout.score.foreach { topLevelScore =>
      if (topLevelScore != "time" && topLevelScore != "reps") {
        errors = errors :+ Errors.scalar(Seq(Message(key = "invalidTopLevelScore", "score" -> topLevelScore)))
      }
    }

    errors
  }

  def validateAncestry(child: core.Workout, parent: Workout): Seq[Errors] = {
    Seq.empty
  }

}
