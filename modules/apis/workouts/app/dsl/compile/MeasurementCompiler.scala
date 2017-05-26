package dsl.workouts.compile

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

import models.workouts.core._

object MeasurementCompiler extends Compiler[Ast.Measurement] {

  def compile(raw: String) = for {
    tokens <- Lexer.lex(raw).right
    ast <- Parser.parse(tokens).right
  } yield ast

  import Ast._

  object Parser extends Parsers {
    override type Elem = Token

    val number = accept("number", {
      case DECIMAL(value) => Number(value)
      case INTEGER(value) => Number(BigDecimal(value))
    })

    val unitOfMeasure = accept(s"known unit of measure [${UnitOfMeasure.values.map(_.symbol).mkString(", ")}]", {
      case REF(text) if UnitOfMeasure.values.map(_.symbol).contains(text) => UnitOfMeasure.values.find(_.symbol == text).get
    })

    val measurement = phrase(number ~ unitOfMeasure) ^^ {
      case number ~ unitOfMeasure => Ast.Measurement(number.value, unitOfMeasure)
    }

    def parse(tokens: Seq[Token]): Either[CompileError, Ast.Measurement] = {
      val reader = new TokenReader(tokens)
      measurement(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

}
