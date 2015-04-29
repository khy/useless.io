package io.useless.play.authentication

class AuthenticationException(msg: String) extends RuntimeException(msg)

class MissingAuthGuidException(configKey: String)
  extends AuthenticationException("Could not find auth GUID for config key [%s]".format(configKey))
