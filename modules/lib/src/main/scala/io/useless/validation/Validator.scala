package io.useless.validation

import scala.util.control.Exception._

import io.useless.Message

object Validator extends Validator("useless.error")

class Validator(prefix: String) {

  def int(raw: String, key: Option[String] = None): Validation[Int] = {
    catching(classOf[NumberFormatException]).
      opt { raw.toInt }.
      map { Validation.Success.apply }.
      getOrElse {
        val message = Message(messageKey("nonInt"), "specified" -> raw)
        Validation.Failure(Seq(Errors(Seq(message), key)))
      }
  }

  private def messageKey(suffix: String) = prefix + "." + suffix

}
