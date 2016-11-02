package io.useless.play.json

import play.api.libs.json._
import play.api.data.validation.ValidationError

import io.useless.{NamedEnum, NamedEnumCompanion}

object NamedEnumJson {

  def keyFormat[T <: NamedEnum](companion: NamedEnumCompanion[T]) = new Format[T] {

    def reads(json: JsValue) = json match {
      case JsString(key) => JsSuccess(companion(key))
      case jsObject: JsObject => (jsObject \ "key").toEither.fold(
        error => JsError(Seq((__ \ "key") -> Seq(error))),
        value => reads(value)
      )
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }

    def writes(namedEnum: T) = JsString(namedEnum.key)

  }

  def fullWrites[T <: NamedEnum] = new Writes[T] {

    def writes(namedEnum: T) = Json.obj(
      "key" -> namedEnum.key,
      "name" -> namedEnum.name
    )

  }

}
