package io.useless.play.json

import java.util.UUID

import play.api.libs.json._
import play.api.data.validation.ValidationError

object UuidJson {

  implicit object UuidJsonFormat extends Format[UUID] {

    def reads(uuid: JsValue): JsResult[UUID] = uuid match {
      case JsString(s) => try { JsSuccess(UUID.fromString(s)) } catch {
        case e: IllegalArgumentException =>
          JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.uuid.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.uuid"))))
    }

    def writes(uuid: UUID): JsValue = JsString(uuid.toString)

  }

}
