package models.workouts.core

import dsl.workouts.compile.ScoreCompiler

class ScoreAst(raw: String) extends Ast {
  val code = raw
}

object ScoreAst {
  implicit val jsonFormat = Ast.jsonFormat(ScoreCompiler)
}
