package dsl.workouts

class ScoreExpression private (raw: String) extends Expression {
  val code = raw
}

object ScoreExpression extends ExpressionCompanion[ScoreExpression] {

  def parse(raw: String) = Right(new ScoreExpression(raw))

  implicit val jsonFormat = Expression.jsonFormat(this)

}
