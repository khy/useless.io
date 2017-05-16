package dsl.workouts

import play.api.libs.json._
import play.api.data.validation.ValidationError

trait Expression {
  def code: String
}

trait ExpressionCompanion[E <: Expression] {
  def parse(raw: String): Either[CompileError, E]
}

object Expression {

  def jsonFormat[E <: Expression](companion: ExpressionCompanion[E]) = new Format[E] {
    def reads(json: JsValue): JsResult[E] = json match {
      case JsString(raw) => companion.parse(raw).fold(
        error => JsError(Seq(JsPath() -> Seq(ValidationError(
          "invalid.expression", error
        )))),
        expression => JsSuccess(expression)
      )
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.string"))))
    }
    def writes(expr: E): JsValue = JsString(expr.code)
  }

}
