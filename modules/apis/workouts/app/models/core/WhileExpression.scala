package models.workouts.core

class WhileExpression private (raw: String) extends Expression {
  val code = raw
}

object WhileExpression extends ExpressionCompanion[WhileExpression] {

  def parse(raw: String) = Right(new WhileExpression(raw))

  implicit val jsonFormat = Expression.jsonFormat(this)

}
