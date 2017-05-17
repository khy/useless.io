package dsl.workouts.compile

import models.workouts.core.ConstraintAst

object ConstraintCompiler extends Compiler[ConstraintAst] {

  def compile(raw: String) = Right(new ConstraintAst(raw))

}
