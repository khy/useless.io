package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

class VariableAst(
  val variable: Ast.Variable
) extends models.workouts.core.Ast {
  def code = "yo"
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

  import Ast._

  object Parser extends Parsers {
    override type Elem = Token

    val field = accept("field", { case field @ FIELD(_) => field })
    val index = accept("index", { case index @ INDEX(_) => index })

    def ref = (field ~ rep(DOT() ~> field | OPEN_BRACKET() ~> index <~ CLOSED_BRACKET())) ^^ { case field ~ ops => {
      def trans(base: String, ops: Seq[Token]): Variable = {
        ops.headOption.map {
          case FIELD(text: String) => ObjectRef(trans(base, ops.tail), text)
          case INDEX(number: Int) => ArrayRef(trans(base, ops.tail), number)
          case token => throw new RuntimeException(s"Unexpected token: ${token}")
        }.getOrElse {
          ImplicitRef(base)
        }
      }

      trans(field.text, ops.reverse)
    }} ^^ { case ref: Variable => new VariableAst(ref) }

    def parse(tokens: Seq[Token]): Either[CompileError, VariableAst] = {
      val reader = new TokenReader(tokens)
      ref(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

}
