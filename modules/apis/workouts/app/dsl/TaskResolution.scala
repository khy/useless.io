package dsl.workouts

import models.workouts._

case class ResolvedTask(
  reps: Option[Int],
  seconds: Option[Int],
  movement: core.TaskMovement
)

object TaskResolver {

  def resolveTasks(workout: core.Workout): Stream[ResolvedTask] = {
    workout.movement.map { movement =>
      val resolvedTask = ResolvedTask(
        reps = workout.reps.map { reps => resolveFormula(reps) },
        seconds = workout.time.map { time => resolveTimeMeasurement(time) },
        movement = movement
      )

      Stream(resolvedTask)
    }.getOrElse {
      workout.tasks.map { tasks =>
        tasks.toStream.flatMap(resolveTasks)
      }.getOrElse(Stream.empty)
    }
  }

  def resolveTasks(subTask: core.SubTask): Stream[ResolvedTask] = {
    subTask.movement.map { movement =>
      val resolvedTask = ResolvedTask(
        reps = subTask.reps.map { reps => resolveFormula(reps) },
        seconds = subTask.time.map { time => resolveTimeMeasurement(time) },
        movement = movement
      )

      Stream(resolvedTask)
    }.getOrElse {
      subTask.tasks.map { tasks =>
        tasks.toStream.flatMap(resolveTasks)
      }.getOrElse(Stream.empty)
    }
  }

  def resolveFormula(formula: Formula): Int = formula match {
    case Formula.Constant(value) => value.toInt
    case Formula.Variable(expression) => throw new RuntimeException("can't handle vars")
  }

  def resolveTimeMeasurement(measurement: Measurement): Int = {
    require(measurement.unitOfMeasure.dimension == Dimension.Time, "must be time measurement")
    measurement.value.toInt
  }

}
