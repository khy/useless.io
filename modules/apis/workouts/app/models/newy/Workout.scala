package models.workouts.newy

import java.util.UUID

case class Workout(
  guid: UUID,
  name: Option[String],
  score: Option[ScoreExpression],
  variables: Option[Seq[FreeVariable]],
  task: AbstractTask
)
