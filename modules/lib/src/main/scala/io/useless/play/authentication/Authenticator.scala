package io.useless.play.authentication

import java.util.UUID
import scala.util.{ Success, Failure }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

import io.useless.accesstoken.AccessToken
import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponents}
import io.useless.util.{ Logger, Uuid }

trait AuthenticatorComponent {

  val authenticator: Authenticator

}

trait Authenticator {

  def authenticate[A](request: Request[A]): Future[Option[AccessToken]]

}

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

abstract class GuidAuthenticator(accessTokenClient: AccessTokenClient) extends Authenticator with Logger {

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

class HeaderAuthenticator(
  accessTokenClient: AccessTokenClient,
  header: String
) extends GuidAuthenticator(accessTokenClient) {

  def guid[A](request: Request[A]) = request.headers.get(header)

}

class QueryParameterAuthenticator(
  accessTokenClient: AccessTokenClient,
  queryParameter: String
) extends GuidAuthenticator(accessTokenClient) {

  def guid[A](request: Request[A]) = request.getQueryString(queryParameter)

}

class CookieAuthenticator(
  accessTokenClient: AccessTokenClient,
  cookie: String
) extends GuidAuthenticator(accessTokenClient) {

  def guid[A](request: Request[A]) = request.cookies.get(cookie).map(_.value)

}
