package models.workouts.core

import dsl.workouts.compile.ConstraintCompiler

class ConstraintAst (raw: String) extends Ast {
  val code = raw
}

object ConstraintAst {
  implicit val jsonFormat = Ast.jsonFormat(ConstraintCompiler)
}
