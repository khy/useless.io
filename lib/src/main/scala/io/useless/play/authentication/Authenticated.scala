package io.useless.play.authentication

object Authenticated
  extends BaseAuthenticated
  with    ClientAuthDaoComponent
{

  val authDao = new ClientAuthDao

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
