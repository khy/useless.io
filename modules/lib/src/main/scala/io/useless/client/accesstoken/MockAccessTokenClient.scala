package io.useless.client.accesstoken

import java.util.UUID
import scala.concurrent.Future

import io.useless.accesstoken.AccessToken

class MockAccessTokenClient(
  accessTokens: Seq[AccessToken]
) extends AccessTokenClient {

  def getAccessToken(guid: UUID) = {
    val optAccessToken = accessTokens.find { _.guid == guid }
    Future.successful(optAccessToken)
  }

}
