package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

object ArithmeticCompiler extends Compiler[Ast.Arithmetic] {

  def compile(source: String) = for {
    tokens <- Lexer.lex(source).right
    ast <- Parser.parse(tokens).right
  } yield ast

  import Ast._

  object Parser extends Parsers with VariableParsers {
    override type Elem = Token

    val number = accept("number", {
      case DECIMAL(value) => Number(value)
      case INTEGER(value) => Number(BigDecimal(value))
    })

    val expr1: Parser[Arithmetic] = number | variable | OPEN_PAREN() ~> expr3 <~ CLOSED_PAREN()

    val expr2: Parser[Arithmetic] = (rep(expr1 ~ TIMES() | expr1 ~ DIVIDED_BY()) ~ expr1) ^^ { case ops ~ last =>
      def tree(last: Arithmetic, ops: Seq[~[Arithmetic, Token]]): Arithmetic = ops match {
        case op :: rest => op match {
          case expr ~ TIMES() => Multiply(tree(expr, rest), last)
          case expr ~ DIVIDED_BY() => Divide(tree(expr, rest), last)
        }
        case _ => last
      }

      tree(last, ops.reverse)
    }

    val expr3: Parser[Arithmetic] = (rep(expr2 ~ PLUS() | expr2 ~ MINUS()) ~ expr2) ^^ { case ops ~ last =>
      def tree(last: Arithmetic, ops: Seq[~[Arithmetic, Token]]): Arithmetic = ops match {
        case op :: rest => op match {
          case number ~ PLUS() => Add(tree(number, rest), last)
          case number ~ MINUS() => Subtract(tree(number, rest), last)
        }
        case _ => last
      }

      tree(last, ops.reverse)
    }

    def parse(tokens: Seq[Token]): Either[CompileError, Arithmetic] = {
      val reader = new TokenReader(tokens)
      expr3(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(expression, next) => Right(expression)
      }
    }
  }

}
