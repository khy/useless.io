package io.useless.play.authentication

import java.util.UUID
import scala.concurrent.Future

import io.useless.accesstoken.AccessToken
import io.useless.client.accesstoken.AccessTokenClient
import io.useless.util.{Configuration, Uuid}

trait AuthDaoComponent {

  def authDao: AuthDao

  trait AuthDao {

    def getAccessToken(guid: UUID): Future[Option[AccessToken]]

  }

}

trait ClientAuthDaoComponent extends AuthDaoComponent with Configuration {

  class ClientAuthDao(authGuid: UUID) extends AuthDao {

    def this(guidConfigKey: String) = this(
      configuration.getString(guidConfigKey).flatMap { raw =>
        Uuid.parseUuid(raw).toOption
      }.getOrElse {
        throw new MissingAuthGuidException(guidConfigKey)
      }
    )

    private lazy val client = AccessTokenClient.instance(authGuid)

    def getAccessToken(guid: UUID) = client.getAccessToken(guid)

  }

}
