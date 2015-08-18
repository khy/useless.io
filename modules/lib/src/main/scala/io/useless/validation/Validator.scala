package io.useless.validation

import scala.util.control.Exception._

object Validator extends Validator("useless.error")

class Validator(prefix: String) {

  def int(key: String, raw: String): Validation[Int] = {
    catching(classOf[NumberFormatException]).
      opt { raw.toInt }.
      map { Validation.success }.
      getOrElse {
        Validation.failure(key, messageKey("nonInt"), "specified" -> raw)
      }
  }

  private def messageKey(suffix: String) = prefix + "." + suffix

}
