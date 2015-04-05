package io.useless.client

import play.api.libs.json.{ JsError, JsValue }

class ClientException(msg: String) extends RuntimeException(msg)

class ServerErrorException(message: String)
  extends ClientException("Server error [%s]".format(message))

class UnauthorizedException(auth: String)
  extends ClientException("Request could not be authorized with [%s]".format(auth))

class UnexpectedStatusException(status: Int, path: String)
  extends ClientException("Unexpected status [%s] for path [%s]".format(status.toString, path))

class InvalidJsonResponseException(value: JsValue, result: JsError)
  extends ClientException("Received an invalid JSON response [%s] with errors [%s]".format(value.toString, result.errors))
