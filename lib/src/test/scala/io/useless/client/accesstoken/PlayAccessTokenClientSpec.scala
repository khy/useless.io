package io.useless.client.accesstoken

import org.scalatest.FunSpec
import org.scalatest.Matchers
import java.util.UUID
import play.api.libs.json._

import io.useless.accesstoken.AccessToken
import io.useless.account.User
import io.useless.play.json.accesstoken.AccessTokenJson._
import io.useless.play.client.MockBaseClientComponent
import io.useless.test.Await

class PlayAccessTokenClientSpec
  extends FunSpec
  with    Matchers
{

  class MockPlayAccessTokenClient(status: Int, json: JsValue)
    extends PlayAccessTokenClient(UUID.randomUUID)
    with    MockBaseClientComponent
  {

    override def baseClient(auth: String) = new MockBaseClient(status, json)

  }

  describe ("PlayAccessTokenClient#getAccessToken") {

    it ("should return an AccessToken if the response is successful and can be parsed") {
      val accessTokenGuid = UUID.fromString("3a65a664-89a0-4f5b-8b9e-f3226af0ff99")
      val accessToken = AccessToken(
        guid = accessTokenGuid,
        resourceOwner = User(
          guid = UUID.randomUUID,
          handle = "khy",
          name = None
        ),
        client = None,
        scopes = Seq()
      )

      val accessTokenClient = new MockPlayAccessTokenClient(200, Json.toJson(accessToken))
      val result = Await(accessTokenClient.getAccessToken(accessTokenGuid)).get
      result.guid should be (accessToken.guid)
      result.resourceOwner.guid should be (accessToken.resourceOwner.guid)
    }

  }

}
