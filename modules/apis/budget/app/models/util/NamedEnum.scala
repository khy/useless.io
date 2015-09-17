package models.budget.util

import play.api.libs.json._
import play.api.data.validation.ValidationError

trait NamedEnum {
  def key: String
  def name: String
}

trait NamedEnumCompanion[T <: NamedEnum] {

  def values: Seq[T]

  def unknown(key: String): T

  def apply(key: String): T = {
    values.find(_.key == key).getOrElse(unknown(key))
  }

}

object NamedEnumJson {

  def keyFormat[T <: NamedEnum](companion: NamedEnumCompanion[T]) = new Format[T] {

    def reads(json: JsValue) = json match {
      case JsString(key) => JsSuccess(companion(key))
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
