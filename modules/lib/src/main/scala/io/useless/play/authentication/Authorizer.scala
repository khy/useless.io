package io.useless.play.authentication

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import io.useless.accesstoken.{ AccessToken, Scope }

trait AuthorizerComponent {

  val authorizer: Authorizer

}

trait Authorizer {

  def authorize(accessToken: AccessToken): Future[Boolean]

}

class CompositeAuthorizer(
  authenticators: Seq[Authorizer]
) extends Authorizer {

  def authorize(accessToken: AccessToken) = {
    val futures = authenticators.map(_.authorize(accessToken))
    Future.reduce(futures)(_ && _)
  }

}

class IdentityAuthorizer extends Authorizer {

  def authorize(accessToken: AccessToken) = Future.successful(true)

}

class ScopeAuthorizer(scopes: Seq[Scope]) extends Authorizer {

  def authorize(accessToken: AccessToken) = Future.successful {
    !scopes.intersect(accessToken.scopes).isEmpty
  }

}
