package models.workouts.core

import play.api.libs.json._
import play.api.data.validation.ValidationError

import dsl.workouts.compile._

case class Code[A <: Ast](
  source: String,
  ast: A
)

object Code {

  private def jsonFormat[A <: Ast](compiler: Compiler[A]) = new Format[Code[A]] {
    def reads(json: JsValue): JsResult[Code[A]] = json match {
      case JsString(source) => compiler.compile(source).fold(
        error => JsError(Seq(JsPath() -> Seq(ValidationError(
          "invalid.code", error
        )))),
        ast => JsSuccess(Code(source, ast))
      )
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.string"))))
    }
    def writes(code: Code[A]): JsValue = JsString(code.source)
  }

  implicit val arithmeticFormat = jsonFormat(ArithmeticCompiler)
  implicit val booleanFormat = jsonFormat(BooleanCompiler)
  implicit val measurementFormat = jsonFormat(MeasurementCompiler)

}
