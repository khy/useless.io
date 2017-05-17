package dsl.workouts.compile

import models.workouts.core.WhileAst

object WhileCompiler extends Compiler[WhileAst] {

  def compile(raw: String) = Right(new WhileAst(raw))

}
