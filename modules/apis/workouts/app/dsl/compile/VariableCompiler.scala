package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

import models.workouts.core.Ast

class VariableAst(
  val ref: VariableAst.Ref
) extends Ast {
  def code = "yo"
}

object VariableAst {
  sealed trait Ref
  case class ImplicitRef(property: String) extends Ref
  case class ObjectRef(ref: Ref, property: String) extends Ref
  case class ArrayRef(ref: Ref, index: Int) extends Ref
}

object VariableCompiler extends Compiler[VariableAst] {

  def compile(raw: String) = for {
    tokens <- Lexer.lex(raw).right
    ast <- Parser.parse(tokens).right
  } yield ast

  sealed trait Token extends Positional
  case class FIELD(text: String) extends Token
  case class DOT() extends Token
  case class OPEN_BRACKET() extends Token
  case class CLOSED_BRACKET() extends Token
  case class INDEX(number: Int) extends Token

  object Lexer extends RegexParsers {
    val field = positioned("[a-z]+".r ^^ { text => FIELD(text) })
    val dot = positioned("." ^^ { _ => DOT() })
    val openBracket = positioned("[" ^^ { _ => OPEN_BRACKET() })
    val closedBracket = positioned("]" ^^ { _ => CLOSED_BRACKET() })
    val index = positioned("""\d+""".r ^^ { text => INDEX(text.toInt) })

    val tokens = phrase(rep1(field | dot | openBracket | closedBracket | index))

    def lex(code: String): Either[CompileError, Seq[Token]] = {
      parse(tokens, code) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

  import VariableAst._

  object Parser extends Parsers {
    override type Elem = Token

    val field = accept("field", { case field @ FIELD(_) => field })
    val index = accept("index", { case index @ INDEX(_) => index })

    def ref = (field ~ rep(DOT() ~> field | OPEN_BRACKET() ~> index <~ CLOSED_BRACKET())) ^^ { case field ~ ops => {
      def trans(base: String, ops: Seq[Token]): Ref = {
        ops.headOption.map {
          case FIELD(text: String) => ObjectRef(trans(base, ops.tail), text)
          case INDEX(number: Int) => ArrayRef(trans(base, ops.tail), number)
          case token => throw new RuntimeException(s"Unexpected token: ${token}")
        }.getOrElse {
          ImplicitRef(base)
        }
      }

      trans(field.text, ops.reverse)
    }} ^^ { case ref: Ref => new VariableAst(ref) }

    def parse(tokens: Seq[Token]): Either[CompileError, VariableAst] = {
      val reader = new TokenReader(tokens)
      ref(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

}
