package io.useless.play.authentication

import io.useless.accesstoken.Scope

object Authorized {

  def apply(scopes: Scope*) = new Authorized(scopes)

}

class Authorized(scopes: Seq[Scope])
  extends BaseAuthenticated
  with    ClientAuthDaoComponent
  with    ScopeAuthorizerComponent
{

  val authDao = new ClientAuthDao

  override val authorizer = new ScopeAuthorizer(scopes)

}
