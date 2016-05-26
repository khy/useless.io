package io.useless.play.json.validation

import play.api.libs.json._

import io.useless.validation.Errors
import io.useless.play.json.MessageJson._

object ErrorsJson {

  implicit val errorsJson = Json.format[Errors]

}
