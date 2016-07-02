package io.useless.validation

import java.util.UUID
import scala.util.control.Exception._
import scala.util.{Success, Failure}
import org.joda.time.DateTime

import io.useless.Message
import io.useless.util.Uuid

object Validator extends Validator("useless.error")

class Validator(prefix: String) {

  def int(raw: String, key: Option[String] = None): Validation[Int] = {
    catching(classOf[NumberFormatException]).
      opt { raw.toInt }.
      map { Validation.Success.apply }.
      getOrElse { failure(key, "nonInt", "specified" -> raw) }
  }

  def long(raw: String, key: Option[String] = None): Validation[Long] = {
    catching(classOf[NumberFormatException]).
      opt { raw.toLong }.
      map { Validation.Success.apply }.
      getOrElse { failure(key, "nonLong", "specified" -> raw) }
  }

  def uuid(raw: String, key: Option[String] = None): Validation[UUID] = {
    Uuid.parseUuid(raw) match {
      case Success(uuid) => Validation.Success(uuid)
      case Failure(_) => failure(key, "nonUuid", "specified" -> raw)
    }
  }

  def dateTime(raw: String, key: Option[String] = None): Validation[DateTime] = {
    catching(classOf[IllegalArgumentException]).
      opt { DateTime.parse(raw) }.
      map { Validation.Success.apply }.
      getOrElse { failure(key, "nonDateTime", "specified" -> raw) }
  }

  def boolean(raw: String, key: Option[String] = None): Validation[Boolean] = raw match {
    case "true" => Validation.success(true)
    case "false" => Validation.success(false)
    case _ => failure(key, "nonBoolean", "specified" -> raw)
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
