package dsl.workouts.compile

object BooleanCompiler extends Compiler[Ast.Boolean] {

  def compile(raw: String) = Right(Ast.TmpBoolean)

}
