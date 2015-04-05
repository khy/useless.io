package io.useless.play.json

import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.ClientError

object ClientErrorJson {

  implicit lazy val format: Format[ClientError] = Format(reads, writes)

  private val reads: Reads[ClientError] = (
    (__ \ "key").read[String] ~
    (__ \ "details").read[JsObject]
  ) { (key: String, details: JsObject) =>
    val _details = details.fields.map { case (key, jsonValue) =>
      (key, jsonValue.as[String])
    }

    new ClientError(key, Map(_details:_*))
  }

  private val writes = new Writes[ClientError] {
    def writes(error: ClientError): JsValue = {
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
