package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

import models.workouts.core._

object MeasurementCompiler extends Compiler[MeasurementAst] {

  def compile(raw: String) = for {
    tokens <- Lexer.lex(raw).right
    ast <- Parser.parse(tokens).right
  } yield ast

  sealed trait Token extends Positional
  case class MAGNITUDE(value: String) extends Token
  case class UNIT(symbol: String) extends Token

  object Lexer extends RegexParsers {
    private val magnitude = positioned("""\d+(\.\d*)?""".r ^^ { str => MAGNITUDE(str) })
    private val unit = positioned("[a-zA-Z]+".r ^^ { str => UNIT(str) })
    private val tokens = phrase(rep1(magnitude | unit))

    def lex(code: String): Either[CompileError, Seq[Token]] = {
      parse(tokens, code) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

  object Parser extends Parsers {
    override type Elem = Token

    private val magnitude = accept("magnitude", {
      case MAGNITUDE(value) if Try(BigDecimal(value)).isSuccess => BigDecimal(value)
    })

    private val unitOfMeasure = accept(s"known unit of measure [${UnitOfMeasure.values.map(_.symbol).mkString(", ")}]", {
      case UNIT(symbol) if UnitOfMeasure.values.map(_.symbol).contains(symbol) => UnitOfMeasure.values.find(_.symbol == symbol).get
    })

    private val measurement = phrase(magnitude ~ unitOfMeasure) ^^ {
      case magnitude ~ unitOfMeasure => new MeasurementAst(magnitude, unitOfMeasure)
    }

    def parse(tokens: Seq[Token]): Either[CompileError, MeasurementAst] = {
      val reader = new TokenReader(tokens)
      measurement(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

}
