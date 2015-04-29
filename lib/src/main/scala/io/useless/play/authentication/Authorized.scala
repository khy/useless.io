package io.useless.play.authentication

import io.useless.accesstoken.Scope

object Authorized {

  def apply(scopes: Scope*) = new Authorized("useless.client.accessTokenGuid", scopes)

}

class Authorized(guidConfigKey: String, scopes: Seq[Scope])
  extends BaseAuthenticated
  with    ClientAuthDaoComponent
  with    ScopeAuthorizerComponent
{

  lazy val authDao = new ClientAuthDao(guidConfigKey)

  override val authorizer = new ScopeAuthorizer(scopes)

}
