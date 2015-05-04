package models.core.account

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.DateTime
import io.useless.accesstoken._

import mongo.AccessToken._

class AccessToken(
  account: Account,
  document: AccessTokenDocument
) {

  val guid = document.guid

  val clientGuid = document.clientGuid

  val scopes = document.scopes

  val createdAt = document.createdAt

  val authorizationCode = document.authorizationCode

  val authorizedAt = document.authorizedAt

  val isAuthorized = authorizedAt.isDefined

  def authorize() = account.authorizeAccessToken(guid)

  val deletedAt = document.deletedAt

  val isDeleted = deletedAt.isDefined

  def delete() = account.deleteAccessToken(guid)

  def toPublic: Future[PublicAccessToken] = toRepresentation { optClient =>
    AccessToken.public(
      guid = guid,
      resourceOwner = account.toPublic,
      client = optClient.map(_.toPublic),
      scopes = scopes
    )
  }

  def toAuthorized: Future[AuthorizedAccessToken] = toRepresentation { optClient =>
    AccessToken.authorized(
      guid = guid,
      authorizationCode = document.authorizationCode,
      resourceOwner = account.toPublic,
      client = optClient.map(_.toPublic),
      scopes = scopes
    )
  }

  private def toRepresentation[T](build: Option[Account] => T) = {
    val futureClient = document.clientGuid.map(Account.forGuid(_)).
      getOrElse(Future.successful(None))

    futureClient.map { client => build(client) }
  }

}
