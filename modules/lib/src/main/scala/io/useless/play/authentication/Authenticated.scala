package io.useless.play.authentication

import play.api.{Application, BuiltInComponents}
import play.api.libs.ws.WS
import play.api.libs.ws.ning.NingWSComponents

import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponents}
import io.useless.util.configuration.RichConfiguration._

trait AuthenticatedComponents {

  self: BuiltInComponents with
    NingWSComponents with
    AccessTokenClientComponents =>

  val authenticated = new Authenticated(accessTokenClient)

}

class LegacyAuthenticated(guidConfigKey: String)(implicit app: Application)
  extends BaseAuthenticated
  with AccessTokenClientComponents
{

  lazy val accessTokenClient = AccessTokenClient.instance(
    client = WS.client,
    baseUrl = app.configuration.underlying.getString("useless.core.baseUrl"),
    authGuid = app.configuration.underlying.getUuid(guidConfigKey)
  )

}

class Authenticated(val accessTokenClient: AccessTokenClient)
  extends BaseAuthenticated
  with AccessTokenClientComponents

trait BaseAuthenticated
  extends AuthenticatedBuilder
  with    AuthenticatorComponent
  with    AuthorizerComponent
  with    RejectorComponent
{

  self: AccessTokenClientComponents =>

  lazy val authenticator: Authenticator = new CompositeAuthenticator(Seq(
    new HeaderAuthenticator(accessTokenClient, "Authorization"),
    new CookieAuthenticator(accessTokenClient, "auth"),
    new QueryParameterAuthenticator(accessTokenClient, "auth")
  ))

  val authorizer: Authorizer = new IdentityAuthorizer

  val rejector: Rejector = new ApiRejector

}
