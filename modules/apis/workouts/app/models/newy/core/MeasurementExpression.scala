package models.workouts.newy.core

class MeasurementExpression private (raw: String) extends Expression {
  val code = raw
}

object MeasurementExpression extends ExpressionCompanion[MeasurementExpression] {

  def parse(raw: String) = Right(new MeasurementExpression(raw))

  implicit val jsonFormat = Expression.jsonFormat(this)

}
