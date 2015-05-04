package io.useless.play.json.accesstoken

import play.api.libs.json._
import play.api.data.validation.ValidationError

import io.useless.accesstoken.Scope

object ScopeJson {

  implicit object ScopeJsonFormat extends Format[Scope] {

    def reads(scope: JsValue): JsResult[Scope] = scope match {
      case JsString(value) => JsSuccess(Scope(value))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.string"))))
    }

    def writes(scope: Scope): JsValue = scope match {
      case Scope(string) => JsString(string)
      case _ => JsNull
    }

  }

}
