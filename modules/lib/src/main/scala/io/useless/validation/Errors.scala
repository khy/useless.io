package io.useless.validation

import io.useless.Message

object Errors {

  def scalar(messages: Seq[Message]) = Errors(messages, key = None)

  def attribute(key: String, messages: Seq[Message]) = Errors(messages, key = Some(key))

}

case class Errors(messages: Seq[Message], key: Option[String])
