package io.useless.play.authentication

import java.util.UUID
import scala.util.{ Success, Failure }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

import io.useless.accesstoken.AccessToken
import io.useless.client.accesstoken.AccessTokenClientComponents
import io.useless.util.{ Logger, Uuid }

trait AuthenticatorComponent {

  val authenticator: Authenticator

  trait Authenticator {

    def authenticate[A](request: Request[A]): Future[Option[AccessToken]]

  }

}

trait CompositeAuthenticatorComponent extends AuthenticatorComponent {

  class CompositeAuthenticator(
    authenticators: Seq[Authenticator]
  ) extends Authenticator {

    def authenticate[A](request: Request[A]): Future[Option[AccessToken]] = {
      val futures = authenticators.map { authenticator =>
        authenticator.authenticate(request)
      }

      Future.sequence(futures).map { results =>
        results.find(_.isDefined).flatten
      }
    }

  }

}

trait GuidAuthenticatorComponent extends AuthenticatorComponent with Logger {

  self: AccessTokenClientComponents =>

  trait GuidAuthenticator extends Authenticator {

    def guid[A](request: Request[A]): Option[String]

    def authenticate[A](request: Request[A]) = {
      guid(request).map { rawGuid =>
        logger.debug("Authenticating guid: %s".format(rawGuid))
        Uuid.parseUuid(rawGuid) match {
          case Success(guid: UUID) => accessTokenClient.getAccessToken(guid)
          case _: Failure[UUID] => Future.successful(None)
        }
      }.getOrElse { Future.successful(None) }
    }

  }

}

trait HeaderAuthenticatorComponent extends GuidAuthenticatorComponent {

  self: AccessTokenClientComponents =>

  class HeaderAuthenticator(header: String) extends GuidAuthenticator {

    def guid[A](request: Request[A]) = request.headers.get(header)

  }

}

trait QueryParameterAuthenticatorComponent extends GuidAuthenticatorComponent {

  self: AccessTokenClientComponents =>

  class QueryParameterAuthenticator(queryParameter: String) extends GuidAuthenticator {

    def guid[A](request: Request[A]) = request.getQueryString(queryParameter)

  }

}

trait CookieAuthenticatorComponent extends GuidAuthenticatorComponent {

  self: AccessTokenClientComponents =>

  class CookieAuthenticator(cookie: String) extends GuidAuthenticator {

    def guid[A](request: Request[A]) = request.cookies.get(cookie).map(_.value)

  }

}
