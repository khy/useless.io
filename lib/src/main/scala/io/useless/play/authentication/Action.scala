package io.useless.play.authentication

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.mvc.Results.Unauthorized

import io.useless.accesstoken.AccessToken

class AuthenticatedRequest[A](
  val accessToken: AccessToken,
  private val request: Request[A]
) extends WrappedRequest[A](request)

trait AuthenticatedBuilder extends ActionBuilder[AuthenticatedRequest] {

  self: AuthenticatorComponent with
        AuthorizerComponent with
        RejectorComponent =>

  def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {
    authenticator.authenticate(request).flatMap { maybeAccessToken =>
      maybeAccessToken.map { accessToken =>
        authorizer.authorize(accessToken).flatMap { authorized =>
          if (authorized) {
            val authenticatedRequest = new AuthenticatedRequest(accessToken, request)
            block(authenticatedRequest)
          } else {
            Future.successful(rejector.unauthorized(request))
          }
        }
      }.getOrElse {
        Future.successful(rejector.unauthenticated(request))
      }
    }
  }

}
