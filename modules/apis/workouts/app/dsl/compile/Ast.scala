package dsl.workouts.compile

import models.workouts.core.UnitOfMeasure

sealed trait Ast

object Ast {

  sealed trait Expression extends Ast

  sealed trait Arithmetic extends Expression

  case class Number(value: BigDecimal) extends Arithmetic
  case class Add(left: Expression, right: Expression) extends Arithmetic
  case class Subtract(left: Expression, right: Expression) extends Arithmetic
  case class Multiply(left: Expression, right: Expression) extends Arithmetic
  case class Divide(left: Expression, right: Expression) extends Arithmetic

  sealed trait Variable extends Expression with Arithmetic

  case class ImplicitRef(property: String) extends Variable
  case class ObjectRef(variable: Variable, property: String) extends Variable
  case class ArrayRef(variable: Variable, index: Int) extends Variable

  sealed trait Boolean extends Expression
  case object TmpBoolean extends Boolean

  case class Measurement(magnitude: BigDecimal, unitOfMeasure: UnitOfMeasure) extends Expression

}
