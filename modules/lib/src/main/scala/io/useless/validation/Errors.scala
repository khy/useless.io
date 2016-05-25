package io.useless.validation

import io.useless.Message

object Errors {

  final case class Scalar(messages: Seq[Message]) extends Seq[Message] with Errors {
    def iterator = messages.iterator
    def apply(idx: Int) = messages.apply(idx)
    def length = messages.length
  }

  final case class Composite(
    principal: Scalar,
    components: Map[String, Errors]
  ) extends Errors

  def scalar(messages: Message*): Scalar = {
    new Scalar(messages)
  }

}

sealed trait Errors {

  import Errors._

  def ++(other: Errors): Errors = {
    (this, other) match {
      case (Scalar(a), Scalar(b)) => Scalar(a ++ b)
      case (Scalar(a), Composite(bp, bc)) => Composite(Scalar(a ++ bp), bc)
      case (Composite(ap, ac), Scalar(b)) => Composite(Scalar(ap ++ b), ac)
      case (Composite(ap, ac), Composite(bp, bc)) => Composite(Scalar(ap ++ bp), ac ++ bc)
    }
  }

}
