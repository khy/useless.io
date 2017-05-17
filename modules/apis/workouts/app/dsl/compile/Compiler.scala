package dsl.workouts.compile

import models.workouts.core.Ast

case class CompileError(column: Int, message: String)

trait Compiler[A <: Ast] {
  def compile(raw: String): Either[CompileError, A]
}
