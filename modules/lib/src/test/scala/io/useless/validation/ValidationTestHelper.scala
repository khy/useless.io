package io.useless.validation

object ValidationTestHelper {

  implicit class RichValidationErrors(errors: Seq[Errors]) {
    def getMessages(key: String) = errors.filter(_.key == Some(key)).head.messages
  }

}
