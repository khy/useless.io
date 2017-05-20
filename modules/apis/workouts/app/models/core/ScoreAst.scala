package models.workouts.core

import dsl.workouts.compile.ScoreCompiler

class ScoreAst(
  raw: String,
  val expression: ScoreAst.Expression
) extends Ast {
  val code = raw
}

object ScoreAst {

  implicit val jsonFormat = Ast.jsonFormat(ScoreCompiler)

  sealed trait Expression

  trait Ref extends Expression
  case class ImplicitRef(property: String) extends Ref
  case class ObjectRef(ref: Ref, property: String) extends Ref
  case class ArrayRef(ref: Ref, index: Int) extends Ref

}
