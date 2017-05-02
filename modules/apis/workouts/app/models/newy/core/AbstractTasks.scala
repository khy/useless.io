package models.workouts.newy.core

import java.util.UUID

case class AbstractTask(
  `while`: WhileExpression,
  movementGuid: Option[UUID],
  constraints: Option[Seq[Constraint]],
  tasks: Option[Seq[AbstractTask]]
)
