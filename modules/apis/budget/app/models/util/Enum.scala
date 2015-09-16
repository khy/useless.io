package models.budget.util

import play.api.libs.json._
import play.api.data.validation.ValidationError

trait Enum {
  def key: String
  def name: String
}

trait EnumCompanion[T <: Enum] {

  def values: Seq[T]

  def unknown(key: String): T

  def apply(key: String): T = {
    values.find(_.key == key).getOrElse(unknown(key))
  }

}

object EnumJson {

  def keyFormat[T <: Enum](enumCompanion: EnumCompanion[T]) = new Format[T] {

    def reads(json: JsValue) = json match {
      case JsString(key) => JsSuccess(enumCompanion(key))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }

    def writes(enum: T) = JsString(enum.key)

  }

  def fullWrites[T <: Enum] = new Writes[T] {

    def writes(enum: T) = Json.obj("key" -> enum.key, "name" -> enum.name)

  }

}
