package models.workouts.old

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
import io.useless.play.json.account.UserJson._

object JsonImplicits {

  implicit object DimensionFormat extends Format[Dimension] {
    def reads(json: JsValue): JsResult[Dimension] = json match {
      case JsString(raw) => Dimension.values.find { dimension =>
        dimension.key == raw
      }.map { dimension =>
        JsSuccess(dimension)
      }.getOrElse {
        JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.dimension.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.string"))))
    }
    def writes(dimension: Dimension): JsValue = JsString(dimension.key)
  }

  implicit object UnitOfMeasureFormat extends Format[UnitOfMeasure] {
    def reads(json: JsValue): JsResult[UnitOfMeasure] = json match {
      case JsString(raw) => UnitOfMeasure.values.find { unitOfMeasure =>
        unitOfMeasure.symbol == raw
      }.map { unitOfMeasure =>
        JsSuccess(unitOfMeasure)
      }.getOrElse {
        JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.unitOfMeasure.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.string"))))
    }
    def writes(unitOfMeasure: UnitOfMeasure): JsValue = JsString(unitOfMeasure.symbol)
  }

  implicit val measurementFormat = Json.format[Measurement]
  implicit val variableFormat = Json.format[Variable]

  implicit val coreMovementFormat = Json.format[core.Movement]
  implicit val coreTaskMovementFormat = Json.format[core.TaskMovement]

  implicit val coreSubTaskFormat: Format[core.SubTask] = (
    (__ \ "reps").formatNullable[Formula] and
    (__ \ "time").formatNullable[Measurement] and
    (__ \ "tasks").lazyFormatNullable(implicitly[Format[Seq[core.SubTask]]]) and
    (__ \ "movement").formatNullable[core.TaskMovement]
  )(core.SubTask.apply, unlift(core.SubTask.unapply))

  implicit val coreWorkoutFormat = Json.format[core.Workout]

  implicit val movementFormat = Json.format[Movement]
  implicit val workoutFormat = Json.format[Workout]

}
