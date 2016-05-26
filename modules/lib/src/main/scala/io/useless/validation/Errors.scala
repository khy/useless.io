package io.useless.validation

import io.useless.Message

object Errors {

  def scalar(messages: Seq[Message]) = Errors(messages, key = None)

  def attribute(key: String, messages: Seq[Message]) = Errors(messages, key = Some(key))

  def combine(a: Seq[Errors], b: Seq[Errors]): Seq[Errors] = {
    val newA = a.map { aErrors =>
      val otherMessages = b.filter(_.key == aErrors.key).flatMap(_.messages)

      if (otherMessages.length > 0) {
        aErrors.copy(messages = (aErrors.messages ++ otherMessages))
      } else aErrors
    }

    val newB = b.filterNot { bErrors => a.map(_.key).contains(bErrors.key) }

    newA ++ newB
  }

}

case class Errors(messages: Seq[Message], key: Option[String])
