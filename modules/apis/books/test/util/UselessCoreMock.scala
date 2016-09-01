package test.util

import java.util.UUID
import io.useless.accesstoken.AccessToken
import io.useless.account.{Account, User}
import io.useless.client.accesstoken.{AccessTokenClient, MockAccessTokenClient}
import io.useless.client.account.{AccountClient, MockAccountClient}

trait UselessCoreMock {

  def accessTokens: Seq[AccessToken]

  lazy val accessTokenClient = new MockAccessTokenClient(accessTokens)

  lazy val accountClient = {
    val accounts = accessTokens.map(_.resourceOwner) ++
      accessTokens.map(_.client).filter(_.isDefined).map(_.get)

    new MockAccountClient(accounts)
  }

}

trait DefaultUselessCoreMock extends UselessCoreMock {

  val khyAccessToken = AccessToken(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    resourceOwner = User(
      guid = UUID.fromString("00000000-1111-1111-1111-111111111111"),
      handle = "khy",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  val mikeAccessToken = AccessToken(
    guid = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    resourceOwner = User(
      guid = UUID.fromString("11111111-2222-2222-2222-222222222222"),
      handle = "mike",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  val dennisAccessToken = AccessToken(
    guid = UUID.fromString("22222222-2222-2222-2222-222222222222"),
    resourceOwner = User(
      guid = UUID.fromString("22222222-3333-3333-3333-333333333333"),
      handle = "dennis",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  val accessTokens = Seq(khyAccessToken, mikeAccessToken, dennisAccessToken)

  implicit val accessToken = accessTokens.head

}
