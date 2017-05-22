package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

import models.workouts.core.Ast

class ArithmeticAst(
  val expression: ArithmeticAst.Expression
) extends Ast {
  def code = "yo"
}

object ArithmeticAst {
  sealed trait Expression
  case class Number(value: BigDecimal) extends Expression
  case class Add(left: Expression, right: Expression) extends Expression
  case class Subtract(left: Expression, right: Expression) extends Expression
}

object ArithmeticCompiler extends Compiler[ArithmeticAst] {

  def compile(raw: String) = for {
    tokens <- Lexer.lex(raw).right
    ast <- Parser.parse(tokens).right
  } yield ast

  sealed trait Token extends Positional
  case class NUMBER(text: String) extends Token
  case class PLUS() extends Token
  case class MINUS() extends Token

  object Lexer extends RegexParsers {
    val number = positioned("""\d+(\.\d*)?""".r ^^ { str => NUMBER(str) })
    val plus = positioned("""\+""".r ^^ { str => PLUS() })
    val minus = positioned("""\-""".r ^^ { str => MINUS() })
    val tokens = phrase(rep1(number | plus | minus))

    def lex(code: String): Either[CompileError, Seq[Token]] = {
      parse(tokens, code) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

  import ArithmeticAst._

  object Parser extends Parsers {
    override type Elem = Token

    val number = accept("number", {
      case NUMBER(text) if Try(BigDecimal(text)).isSuccess => Number(BigDecimal(text))
    })

    val plusMinus = (rep(number ~ PLUS() | number ~ MINUS()) ~ number) ^^ { case ops ~ last =>
      def trans(last: Number, ops: Seq[~[Number, Token]]): Expression = ops match {
        case op :: rest => op match {
          case number ~ PLUS() => Add(trans(number, rest), last)
          case number ~ MINUS() => Subtract(trans(number, rest), last)
        }
        case _ => last
      }

      trans(last, ops.reverse)
    }

    def parse(tokens: Seq[Token]): Either[CompileError, ArithmeticAst] = {
      val reader = new TokenReader(tokens)
      plusMinus(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(expression, next) => Right(new ArithmeticAst(expression))
      }
    }
  }

}
