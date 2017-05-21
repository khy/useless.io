package dsl.workouts.compile

import scala.util.parsing.combinator._
import scala.util.parsing.input._

import models.workouts.core.ScoreAst
import models.workouts.core.ScoreAst._

object ScoreCompiler extends Compiler[ScoreAst] {

  def compile(raw: String) = for {
    tokens <- Lexer.lex(raw).right
    ast <- Parser.parse(tokens).right
  } yield ast

  sealed trait Token extends Positional
  case class Field(text: String) extends Token
  case class Dot() extends Token
  case class OpenBracket() extends Token
  case class ClosedBracket() extends Token
  case class Index(number: Int) extends Token
  case class Plus() extends Token

  private object Lexer extends RegexParsers {
    val field = positioned("[a-z]+".r ^^ { text => Field(text) })
    val dot = positioned("""\.{1}""".r ^^ { _ => Dot() })
    val openBracket = positioned("""\[{1}""".r ^^ { _ => OpenBracket() })
    val closedBracket = positioned("""\]{1}""".r ^^ { _ => ClosedBracket() })
    val index = positioned("""\d+""".r ^^ { text => Index(text.toInt) })
    val plus = positioned("""\+{1}""".r ^^ { text => Plus() })

    val tokens = phrase(rep1(field | dot | openBracket | closedBracket | index | plus))

    def lex(code: String): Either[CompileError, Seq[Token]] = {
      parse(tokens, code) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

  private object Parser extends Parsers with PackratParsers {
    override type Elem = Token

    val field = accept("field", { case field @ Field(_) => field })
    val index = accept("index", { case index @ Index(_) => index })

    sealed trait Access
    case class PropAccess(prop: String) extends Access
    case class IndexAccess(index: Int) extends Access

    def accesses: Parser[Seq[Access]] = {
      val propAccess = (Dot() ~ field) ^^ { case _ ~ field => PropAccess(field.text) }
      val indexAccess = (OpenBracket() ~ index ~ ClosedBracket()) ^^ { case _ ~ index ~ _ => IndexAccess(index.number)}
      rep(propAccess | indexAccess)
    }

    def ref = (field ~ accesses) ^^ { case field ~ accesses => {
      def trans(base: String, accesses: Seq[Access]): ScoreAst.Ref = {
        accesses.headOption.map {
          case PropAccess(prop: String) => ScoreAst.ObjectRef(trans(base, accesses.tail), prop)
          case IndexAccess(index: Int) => ScoreAst.ArrayRef(trans(base, accesses.tail), index)
        }.getOrElse {
          ScoreAst.ImplicitRef(base)
        }
      }

      trans(field.text, accesses.reverse)
    }}

    def op = (ref ~ Plus() ~ ref) ^^ { case leftRef ~ plus ~ rightRef => ScoreAst.AdditionOp(leftRef, rightRef) }

    lazy val expression = (op | ref) ^^ { case expression: Expression => new ScoreAst("hi", expression) }

    def parse(tokens: Seq[Token]): Either[CompileError, ScoreAst] = {
      val reader = new TokenReader(tokens)
      expression(reader) match {
        case NoSuccess(msg, next) => Left(CompileError(next.pos.column, msg))
        case Success(result, next) => Right(result)
      }
    }
  }

}
