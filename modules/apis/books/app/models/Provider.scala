package models.books

import play.api.libs.json._
import play.api.data.validation.ValidationError

sealed trait Provider {
  def key: String
}

object Provider {

  def apply(key: String): Provider = key match {
    case "google" => Google
    case key => Unknown(key)
  }

  case object Google extends Provider {
    val key = "google"
  }

  case class Unknown(key: String) extends Provider

  implicit object ProviderFormat extends Format[Provider] {

    def reads(js: JsValue): JsResult[Provider] = js match {
      case JsString(key) => JsSuccess(Provider(key))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.string"))))
    }

    def writes(provider: Provider): JsValue = JsString(provider.key)

  }

}
