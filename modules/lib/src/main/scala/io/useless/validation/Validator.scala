package io.useless.validation

import java.util.UUID
import scala.util.control.Exception._
import scala.util.{Success, Failure}

import io.useless.Message
import io.useless.util.Uuid

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

  def uuid(raw: String): Seq[Message] = {
    Uuid.parseUuid(raw) match {
      case Success(uuid) => Seq.empty
      case Failure(_) => Seq(Message(messageKey("nonUuid"), "specified" -> raw))
    }
  }

  private def messageKey(suffix: String) = prefix + "." + suffix

}
