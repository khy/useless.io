package io.useless.play.authentication

import play.api.{Application, BuiltInComponents}
import play.api.libs.ws.WS
import play.api.libs.ws.ning.NingWSComponents

import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponent}
import io.useless.util.configuration.RichConfiguration._

trait AuthenticatedComponent {

  self: BuiltInComponents with
    NingWSComponents with
    AccessTokenClientComponent =>

  val authenticated = new Authenticated(accessTokenClient)

}

class LegacyAuthenticated(guidConfigKey: String)(implicit app: Application) extends BaseAuthenticated {

  lazy val accessTokenClient = AccessTokenClient.instance(
    client = WS.client,
    baseUrl = app.configuration.underlying.getString("useless.core.baseUrl"),
    authGuid = app.configuration.underlying.getUuid(guidConfigKey)
  )

}

class Authenticated(protected val accessTokenClient: AccessTokenClient)
  extends BaseAuthenticated

trait BaseAuthenticated
  extends AuthenticatedBuilder
  with    CompositeAuthenticatorComponent
  with    HeaderAuthenticatorComponent
  with    CookieAuthenticatorComponent
  with    QueryParameterAuthenticatorComponent
  with    IdentityAuthorizerComponent
  with    ApiRejectorComponent
{

  val authenticator: Authenticator = new CompositeAuthenticator(Seq(
    new HeaderAuthenticator("Authorization"),
    new CookieAuthenticator("auth"),
    new QueryParameterAuthenticator("auth")
  ))

  val authorizer: Authorizer = new IdentityAuthorizer

  val rejector: Rejector = new ApiRejector

}
