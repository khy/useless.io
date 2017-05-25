package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

object VariableCompiler extends Compiler[Ast.Variable] {

  def compile(source: String) = for {
    tokens <- Lexer.lex(source).right
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

    def variable = (field ~ rep(DOT() ~> field | OPEN_BRACKET() ~> index <~ CLOSED_BRACKET())) ^^ { case field ~ ops => {
      def tree(base: String, ops: Seq[Token]): Variable = {
        ops.headOption.map {
          case FIELD(text: String) => ObjectRef(tree(base, ops.tail), text)
          case INDEX(number: Int) => ArrayRef(tree(base, ops.tail), number)
          case token => throw new RuntimeException(s"Unexpected token: ${token}")
        }.getOrElse {
          ImplicitRef(base)
        }
      }

      tree(field.text, ops.reverse)
    }}

    def parse(tokens: Seq[Token]): Either[CompileError, Ast.Variable] = {
      val reader = new TokenReader(tokens)
      variable(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

}
