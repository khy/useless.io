package models.workouts.core

import dsl.workouts.compile.WhileCompiler

class WhileAst(raw: String) extends Ast {
  val code = raw
}

object WhileAst {
  implicit val jsonFormat = Ast.jsonFormat(WhileCompiler)
}
