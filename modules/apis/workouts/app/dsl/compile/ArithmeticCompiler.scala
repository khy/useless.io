package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

object ArithmeticCompiler extends Compiler[Ast.Arithmetic] {

  def compile(source: String) = for {
    tokens <- Lexer.lex(source).right
    ast <- Parser.parse(tokens).right
  } yield ast

  sealed trait Token extends Positional
  case class NUMBER(text: String) extends Token
  case class PLUS() extends Token
  case class MINUS() extends Token
  case class TIMES() extends Token
  case class DIVIDED_BY() extends Token
  case class OPEN_PAREN() extends Token
  case class CLOSED_PAREN() extends Token

  object Lexer extends RegexParsers {
    val number = positioned("""\d+(\.\d*)?""".r ^^ { str => NUMBER(str) })
    val plus = positioned("+" ^^ { str => PLUS() })
    val minus = positioned("-" ^^ { str => MINUS() })
    val times = positioned("*" ^^ { str => TIMES() })
    val dividedBy = positioned("\\" ^^ { str => DIVIDED_BY() })
    val openParen = positioned("(" ^^ { str => OPEN_PAREN() })
    val closedParen = positioned(")" ^^ { str => CLOSED_PAREN() })
    val tokens = phrase(rep1(number | plus | minus | times | dividedBy | openParen | closedParen))

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

    val number = accept("number", {
      case NUMBER(text) if Try(BigDecimal(text)).isSuccess => Number(BigDecimal(text))
    })

    val expr1: Parser[Arithmetic] = number | OPEN_PAREN() ~> expr3 <~ CLOSED_PAREN()

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
