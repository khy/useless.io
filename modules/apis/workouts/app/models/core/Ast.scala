package models.workouts.core

import play.api.libs.json._
import play.api.data.validation.ValidationError

import dsl.workouts.compile.Compiler

trait Ast {
  def code: String
}

object Ast {

  def jsonFormat[A <: Ast](compiler: Compiler[A]) = new Format[A] {
    def reads(json: JsValue): JsResult[A] = json match {
      case JsString(raw) => compiler.compile(raw).fold(
        error => JsError(Seq(JsPath() -> Seq(ValidationError(
          "invalid.expression", error
        )))),
        ast => JsSuccess(ast)
      )
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.string"))))
    }
    def writes(ast: A): JsValue = JsString(ast.code)
  }

}
