package dsl.workouts.compile

import models.workouts.core.ScoreAst

object ScoreCompiler extends Compiler[ScoreAst] {

  def compile(raw: String) = Right(new ScoreAst(raw))

}
