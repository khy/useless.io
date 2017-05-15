package models.workouts.core

import scala.util.Try
import scala.util.parsing.combinator._
import scala.util.parsing.input._

class MeasurementExpression private (
  magnitude: BigDecimal,
  unitOfMeasure: UnitOfMeasure
) extends Expression {
  val code = s"${magnitude} ${unitOfMeasure.symbol}"
}

object MeasurementExpression extends ExpressionCompanion[MeasurementExpression] {

  sealed trait Token
  case class MAGNITUDE(value: String) extends Token
  case class UNIT(symbol: String) extends Token

  object Lexer extends RegexParsers {
    private val magnitude = """\d+(\.\d*)?""".r ^^ { str => MAGNITUDE(str) }
    private val unit = "[a-zA-Z]+".r ^^ { str => UNIT(str) }
    private val tokens = phrase(rep1(magnitude | unit))

    def lex(code: String): Either[String, Seq[Token]] = {
      parse(tokens, code) match {
        case NoSuccess(msg, next) => Left(msg)
        case Success(result, next) => Right(result)
      }
    }
  }

  class SeqReader[T](elems: Seq[T]) extends Reader[T] {
    override def first: T = elems.head
    override def atEnd: Boolean = elems.isEmpty
    override def pos: Position = NoPosition
    override def rest: Reader[T] = new SeqReader(elems.tail)
  }

  case class Ast(magnitude: BigDecimal, unitOfMeasure: UnitOfMeasure)

  object Parser extends Parsers {
    override type Elem = Token

    private val magnitude = accept("magnitude", {
      case MAGNITUDE(value) if Try(BigDecimal(value)).isSuccess => BigDecimal(value)
    })

    private val unitOfMeasure = accept(s"known unit of measure [${UnitOfMeasure.values.map(_.symbol).mkString(", ")}]", {
      case UNIT(symbol) if UnitOfMeasure.values.map(_.symbol).contains(symbol) => UnitOfMeasure.values.find(_.symbol == symbol).get
    })

    private val measurement = phrase(magnitude ~ unitOfMeasure) ^^ {
      case magnitude ~ unitOfMeasure => Ast(magnitude, unitOfMeasure)
    }

    def parse(tokens: Seq[Token]): Either[String, Ast] = {
      val reader = new SeqReader(tokens)
      measurement(reader) match {
        case NoSuccess(msg, next) => Left(msg)
        case Success(result, next) => Right(result)
      }
    }
  }

  def parse(raw: String) = for {
    tokens <- Lexer.lex(raw).right
    ast <- Parser.parse(tokens).right
  } yield new MeasurementExpression(ast.magnitude, ast.unitOfMeasure)

  implicit val jsonFormat = Expression.jsonFormat(this)

}
