package models.workouts.core

import play.api.libs.json._
import play.api.data.validation.ValidationError

case class Dimension(key: String)

object Dimension {
  val Angle = Dimension("angle")
  val Distance = Dimension("distance")
  val Time = Dimension("time")
  val Weight = Dimension("weight")

  val values = Seq(Angle, Distance, Time, Weight)

  implicit val jsonFormat = new Format[Dimension] {
    def reads(json: JsValue): JsResult[Dimension] = json match {
      case JsString(raw) => values.find { dimension =>
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
}
