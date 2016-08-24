package io.useless.play.authentication

import play.api.{Application, BuiltInComponents}
import play.api.libs.ws.ning.NingWSComponents

import io.useless.accesstoken.Scope
import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponent}

trait AuthorizedComponent {

  self: BuiltInComponents with
    NingWSComponents with
    AccessTokenClientComponent =>

  def authorized(scopes: Seq[Scope]) = {
    new Authorized(accessTokenClient, scopes)
  }

}

class Authorized(accessTokenClient: AccessTokenClient, scopes: Seq[Scope])
  extends Authenticated(accessTokenClient)
  with ScopeAuthorizerComponent
{

  override val authorizer = new ScopeAuthorizer(scopes)

}
