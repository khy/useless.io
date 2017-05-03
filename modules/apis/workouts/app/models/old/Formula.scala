package models.workouts.old

import scala.util.control.Exception._
import play.api.libs.json._
import play.api.data.validation.ValidationError

sealed trait Formula

object Formula {

  case class Constant(value: BigDecimal) extends Formula
  case class Variable(expression: String) extends Formula

  implicit val format = new Format[Formula] {
    def reads(json: JsValue): JsResult[Formula] = json match {
      case JsString(raw) => {
        val optInt = catching(classOf[NumberFormatException]) opt BigDecimal(raw)
        val jsValue = optInt.map { value => Constant(value) } getOrElse { Variable(raw) }
        JsSuccess(jsValue)
      }
      case JsNumber(value) => JsSuccess(Constant(value))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.stringOrNumber"))))
    }

    def writes(formula: Formula): JsValue = formula match {
      case Formula.Constant(value) => JsNumber(value)
      case Formula.Variable(expression) => JsString(expression)
    }
  }

}
