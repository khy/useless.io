package io.useless.util

object Validator {

  // Copied from https://github.com/playframework/playframework/blob/516cc89e398ffadf25a6773cd04f2f91edc910c2/framework/src/play/src/main/scala/play/api/data/validation/Validation.scala#L76
  private lazy val emailRegex = """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r

  def isValidEmail(email: String) = {
    emailRegex.findFirstMatchIn(email).isDefined
  }

  private lazy val handleRegex = """^(?:[a-z]|\d|-)*$""".r

  def isValidHandle(handle: String) = {
    handleRegex.findFirstMatchIn(handle).isDefined
  }

}
