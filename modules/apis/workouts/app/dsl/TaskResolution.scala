package dsl.workouts

import models.workouts._

case class ResolvedTask(
  reps: Option[Int],
  seconds: Option[Int],
  movement: core.TaskMovement
)

object TaskResolver {

  def resolveTasks(task: Task): Stream[ResolvedTask] = {
    task.movement.map { movement =>
      val resolvedTask = ResolvedTask(
        reps = task.reps.map { reps => resolveFormula(reps) },
        seconds = task.time.map { time => resolveTimeMeasurement(time) },
        movement = movement
      )

      Stream(resolvedTask)
    }.getOrElse {
      task.reps.map { reps =>
        val resolvedReps = resolveFormula(reps)

        Range(0, resolvedReps).toStream.flatMap { rep =>
          task.tasks.map { tasks =>
            tasks.toStream.flatMap(resolveTasks)
          }.getOrElse(Stream.empty)
        }
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
