package models.workouts.core

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import dsl.workouts.WhileExpression

case class AbstractTask(
  `while`: WhileExpression,
  movement: Option[UUID],
  constraints: Option[Seq[Constraint]],
  tasks: Option[Seq[AbstractTask]]
)

object AbstractTask {

  implicit val jsonFormat: Format[AbstractTask] = (
    (__ \ "while").format[WhileExpression] and
    (__ \ "movement").formatNullable[UUID] and
    (__ \ "constraints").formatNullable[Seq[Constraint]] and
    (__ \ "tasks").lazyFormatNullable(implicitly[Format[Seq[AbstractTask]]])
  )(AbstractTask.apply, unlift(AbstractTask.unapply))

}
