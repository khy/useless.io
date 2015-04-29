package io.useless.play.authentication

import java.util.UUID
import scala.concurrent.Future

import io.useless.accesstoken.AccessToken
import io.useless.client.accesstoken.AccessTokenClient

trait AuthDaoComponent {

  def authDao: AuthDao

  trait AuthDao {

    def getAccessToken(guid: UUID): Future[Option[AccessToken]]

  }

}

trait ClientAuthDaoComponent extends AuthDaoComponent {

  class ClientAuthDao(optAuthGuid: Option[UUID] = None) extends AuthDao {

    private lazy val client = AccessTokenClient.instance(optAuthGuid)

    def getAccessToken(guid: UUID) = client.getAccessToken(guid)

  }

}
