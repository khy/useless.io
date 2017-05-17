package models.workouts.core

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class AbstractTask(
  `while`: WhileAst,
  movement: Option[UUID],
  constraints: Option[Seq[Constraint]],
  tasks: Option[Seq[AbstractTask]]
)

object AbstractTask {

  implicit val jsonFormat: Format[AbstractTask] = (
    (__ \ "while").format[WhileAst] and
    (__ \ "movement").formatNullable[UUID] and
    (__ \ "constraints").formatNullable[Seq[Constraint]] and
    (__ \ "tasks").lazyFormatNullable(implicitly[Format[Seq[AbstractTask]]])
  )(AbstractTask.apply, unlift(AbstractTask.unapply))

}
