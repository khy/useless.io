package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

object VariableCompiler extends Compiler[Ast.Variable] {

  def compile(source: String) = for {
    tokens <- Lexer.lex(source).right
    ast <- Parser.parse(tokens).right
  } yield ast

  object Parser extends Parsers with VariableParsers {
    def parse(tokens: Seq[Token]): Either[CompileError, Ast.Variable] = {
      val reader = new TokenReader(tokens)
      variable(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

}

trait VariableParsers {

  self: Parsers =>

  import Ast._

  override type Elem = Token

  val ref = accept("ref", { case ref @ REF(_) => ref })
  val integer = accept("integer", { case integer @ INTEGER(_) => integer })

  def variable = (ref ~ rep(DOT() ~> ref | OPEN_BRACKET() ~> integer <~ CLOSED_BRACKET())) ^^ { case ref ~ ops => {
    def tree(base: String, ops: Seq[Token]): Variable = {
      ops.headOption.map {
        case REF(text: String) => ObjectRef(tree(base, ops.tail), text)
        case INTEGER(value: Int) => ArrayRef(tree(base, ops.tail), value)
        case token => throw new RuntimeException(s"Unexpected token: ${token}")
      }.getOrElse {
        ImplicitRef(base)
      }
    }

    tree(ref.text, ops.reverse)
  }}

}
