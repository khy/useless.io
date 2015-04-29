package io.useless.play.authentication

import java.util.UUID

import io.useless.util.Configuration

object Authenticated extends Authenticated("useless.client.accessTokenGuid")

class Authenticated(guidConfigKey: String)
  extends BaseAuthenticated
  with    ClientAuthDaoComponent
  with    Configuration
{

  val authDao = new ClientAuthDao(
    configuration.getString(guidConfigKey).map { raw =>
      UUID.fromString(raw)
    }
  )

}

trait BaseAuthenticated
  extends AuthenticatedBuilder
  with    CompositeAuthenticatorComponent
  with    HeaderAuthenticatorComponent
  with    CookieAuthenticatorComponent
  with    QueryParameterAuthenticatorComponent
  with    IdentityAuthorizerComponent
  with    ApiRejectorComponent
{

  self: AuthDaoComponent =>

  val authenticator: Authenticator = new CompositeAuthenticator(Seq(
    new HeaderAuthenticator("Authorization"),
    new CookieAuthenticator("auth"),
    new QueryParameterAuthenticator("auth")
  ))

  val authorizer: Authorizer = new IdentityAuthorizer

  val rejector: Rejector = new ApiRejector

}
