package helpers.auth

import play.api.data.FormError

object Form {

  def errorMessageWithContext(error: FormError) = {
    error.message match {
      case "error.required" => error.key.capitalize + " is required."
      case "error.email" => "Valid email is required."
      case "error.invalid-sign-in" => "Email and / or password was incorrect."
      case other => other
    }
  }

}
