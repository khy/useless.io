package dsl.workouts.compile

case class CompileError(column: Int, message: String)

trait Compiler[A <: Ast] {
  def compile(source: String): Either[CompileError, A]
}
