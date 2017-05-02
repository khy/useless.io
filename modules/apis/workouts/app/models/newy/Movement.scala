package models.workouts.newy

import java.util.UUID

case class Movement(
  guid: UUID,
  name: String,
  variables: Option[Seq[FreeVariable]]
)
