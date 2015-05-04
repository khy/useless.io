package io.useless.play.json

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.data.validation.ValidationError

import io.useless.util.Format

object DateTimeJson {

  implicit object DateTimeJsonFormat extends Format[DateTime] {

    def reads(dateTime: JsValue): JsResult[DateTime] = dateTime match {
      case JsString(s) => try { JsSuccess(DateTime.parse(s)) } catch {
        case e: IllegalArgumentException =>
          JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date_time.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date_time"))))
    }

    def writes(dateTime: DateTime): JsValue = JsString(Format.dateTime(dateTime))

  }

}
