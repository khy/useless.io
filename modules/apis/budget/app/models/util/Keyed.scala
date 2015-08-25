package models.budget.util

import play.api.libs.json._
import play.api.data.validation.ValidationError

trait Keyed {
  def key: String
}

trait KeyedResolver[T <: Keyed] {

  def values: Seq[T]

  def unknown(key: String): T

  def apply(key: String): T = {
    values.find(_.key == key).getOrElse(unknown(key))
  }

}

object KeyedJson {

  def format[T <: Keyed](keyedResolver: KeyedResolver[T]) = new Format[T] {

    def reads(json: JsValue) = json match {
      case JsString(key) => JsSuccess(keyedResolver(key))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }

    def writes(keyed: T) = JsString(keyed.key)

  }

}
