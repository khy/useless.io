package io.useless.play.authentication

import play.api.Application

import io.useless.accesstoken.Scope

object Authorized {

  def apply(scopes: Scope*)(implicit app: Application) = new Authorized("useless.client.accessTokenGuid", scopes)

}

class Authorized(guidConfigKey: String, scopes: Seq[Scope])(implicit app: Application)
  extends BaseAuthenticated
  with    ClientAuthDaoComponent
  with    ScopeAuthorizerComponent
{

  lazy val authDao = new ClientAuthDao(guidConfigKey)

  override val authorizer = new ScopeAuthorizer(scopes)

}
