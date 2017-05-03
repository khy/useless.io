package models.workouts.core

class ConstraintExpression private (raw: String) extends Expression {
  val code = raw
}

object ConstraintExpression extends ExpressionCompanion[ConstraintExpression] {

  def parse(raw: String) = Right(new ConstraintExpression(raw))

  implicit val jsonFormat = Expression.jsonFormat(this)

}
