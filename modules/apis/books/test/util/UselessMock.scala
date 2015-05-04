package test.util

import io.useless.accesstoken.AccessToken
import io.useless.account.Account
import io.useless.client.accesstoken.{ AccessTokenClient, MockAccessTokenClient }
import io.useless.client.account.{ AccountClient, MockAccountClient }

trait UselessMock {

  def accessTokens: Seq[AccessToken]

  lazy val mockAccessTokenClient = new MockAccessTokenClient(accessTokens)

  lazy val mockAccountClient = {
    val accounts = accessTokens.map(_.resourceOwner) ++
      accessTokens.map(_.client).filter(_.isDefined).map(_.get)

    new MockAccountClient(accounts)
  }

  def setMocks() = {
    AccessTokenClient.setMock(mockAccessTokenClient)
    AccountClient.setMock(mockAccountClient)
  }

}
