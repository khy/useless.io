package models.workouts.core

import dsl.workouts.compile.MeasurementCompiler

class MeasurementAst (
  val magnitude: BigDecimal,
  val unitOfMeasure: UnitOfMeasure
) extends Ast {
  val code = s"${magnitude} ${unitOfMeasure.symbol}"
}

object MeasurementAst {
  implicit val jsonFormat = Ast.jsonFormat(MeasurementCompiler)
}
