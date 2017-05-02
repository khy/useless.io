package models.workouts.newy.core

import java.util.UUID

case class Movement(
  name: String,
  variables: Option[Seq[FreeVariable]]
)
