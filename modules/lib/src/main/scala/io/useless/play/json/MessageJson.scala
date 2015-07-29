package io.useless.play.json

import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.Message

object MessageJson {

  implicit lazy val format: Format[Message] = Format(reads, writes)

  private val reads: Reads[Message] = (
    (__ \ "key").read[String] ~
    (__ \ "details").read[JsObject]
  ) { (key: String, details: JsObject) =>
    val _details = details.fields.map { case (key, jsonValue) =>
      (key, jsonValue.as[String])
    }

    new Message(key, Map(_details:_*))
  }

  private val writes = new Writes[Message] {
    def writes(error: Message): JsValue = {
      val _details = error.details.map { case (key, value) =>
        (key, Json.toJsFieldJsValueWrapper(value))
      }

      Json.obj(
        "key" -> error.key,
        "details" -> Json.obj(_details.toSeq:_*)
      )
    }
  }

}
