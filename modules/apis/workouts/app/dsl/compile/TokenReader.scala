package dsl.workouts.compile

import scala.util.parsing.combinator._
import scala.util.parsing.input._

class TokenReader[T <: Positional](elems: Seq[T]) extends Reader[T] {
  override def first: T = elems.head
  override def atEnd: Boolean = elems.isEmpty
  override def pos: Position = elems.headOption.map(_.pos).getOrElse(NoPosition)
  override def rest: Reader[T] = new TokenReader(elems.tail)
}
