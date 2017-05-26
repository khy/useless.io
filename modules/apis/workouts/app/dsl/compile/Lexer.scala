package dsl.workouts.compile

import scala.util.parsing.combinator._
import scala.util.parsing.input._

object Lexer extends RegexParsers {

  val number = positioned("""\d+(\.\d+)?""".r ^^ { str =>
    if (str.contains(".")) DECIMAL(BigDecimal(str)) else INTEGER(str.toInt)
  })

  val ref = positioned("[a-z]+".r ^^ { str => REF(str) })
  val plus = positioned("+" ^^ { _ => PLUS() })
  val minus = positioned("-" ^^ { _ => MINUS() })
  val times = positioned("*" ^^ { _ => TIMES() })
  val dividedBy = positioned("\\" ^^ { _ => DIVIDED_BY() })
  val openParen = positioned("(" ^^ { _ => OPEN_PAREN() })
  val closedParen = positioned(")" ^^ { _ => CLOSED_PAREN() })
  val openBracket = positioned("[" ^^ { _ => OPEN_BRACKET() })
  val closedBracket = positioned("]" ^^ { _ => CLOSED_BRACKET() })
  val dot = positioned("." ^^ { _ => DOT() })

  val tokens = phrase(rep1(number | ref | plus | minus | times |
    dividedBy | openParen | closedParen | openBracket | closedBracket | dot))

  def lex(code: String): Either[CompileError, Seq[Token]] = {
    parse(tokens, code) match {
      case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
      case Success(result, next) => Right(result)
    }
  }

}

sealed trait Token extends Positional

case class DECIMAL(value: BigDecimal) extends Token
case class INTEGER(value: Int) extends Token
case class REF(text: String) extends Token
case class PLUS() extends Token
case class MINUS() extends Token
case class TIMES() extends Token
case class DIVIDED_BY() extends Token
case class OPEN_PAREN() extends Token
case class CLOSED_PAREN() extends Token
case class OPEN_BRACKET() extends Token
case class CLOSED_BRACKET() extends Token
case class DOT() extends Token
