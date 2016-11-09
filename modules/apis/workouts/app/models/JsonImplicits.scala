package models.workouts

import play.api.libs.json._
import play.api.data.validation.ValidationError

object JsonImplicits {

  implicit object DimensionFormat extends Format[Dimension] {
    def reads(json: JsValue): JsResult[Dimension] = json match {
      case JsString(raw) => raw match {
        case "angle" => JsSuccess(Dimension.Angle)
        case "distance" => JsSuccess(Dimension.Distance)
        case "time" => JsSuccess(Dimension.Time)
        case "weight" => JsSuccess(Dimension.Weight)
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.dimension.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.string"))))
    }

    def writes(dimension: Dimension): JsValue = dimension match {
      case Dimension.Angle => JsString("angle")
      case Dimension.Distance => JsString("distance")
      case Dimension.Time => JsString("time")
      case Dimension.Weight => JsString("weight")
    }
  }

  implicit val unitOfMeasureFormat = Json.format[UnitOfMeasure]
  implicit val measurementFormat = Json.format[Measurement]
  implicit val variableFormat = Json.format[Variable]
  implicit val movementFormat = Json.format[Movement]

}
