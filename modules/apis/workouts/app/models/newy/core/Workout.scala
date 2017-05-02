package models.workouts.newy.core

import java.util.UUID

case class Workout(
  name: Option[String],
  score: Option[ScoreExpression],
  variables: Option[Seq[FreeVariable]],
  task: AbstractTask
)
