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
      getOrElse { failure(key, "nonInt", "specified" -> raw) }
  }

  def uuid(raw: String, key: Option[String] = None): Validation[UUID] = {
    Uuid.parseUuid(raw) match {
      case Success(uuid) => Validation.Success(uuid)
      case Failure(_) => failure(key, "nonUuid", "specified" -> raw)
    }
  }

  private def failure(
    _errorKey: Option[String],
    _messageKey: String,
    details: (String, String)*
  ) = {
    val message = Message(messageKey(_messageKey), details:_*)
    Validation.Failure(Seq(Errors(Seq(message), _errorKey)))
  }

  private def messageKey(suffix: String) = prefix + "." + suffix

}
