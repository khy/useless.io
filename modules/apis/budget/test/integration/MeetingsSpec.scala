package test.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WS
import play.api.libs.json._
import org.joda.time.DateTime

import io.useless.accesstoken.AccessToken
import io.useless.account.{Account, User}
import io.useless.client.accesstoken.{AccessTokenClient, MockAccessTokenClient}
import io.useless.client.account.{AccountClient, MockAccountClient}

class MeetingsSpec
  extends PlaySpec
  with OneServerPerSuite
{

  "POST /meetings" must {

    val meetingJson = Json.obj("date" -> DateTime.now)

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await {
        WS.url(s"http://localhost:$port/meetings").post(meetingJson)
      }

      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict if date isn't specified" in {
      val response = await {
        WS.url(s"http://localhost:$port/meetings").
          withHeaders(("Authorization" -> accessToken.guid.toString)).
          post(Json.obj())
      }

      response.status mustBe CONFLICT
    }

    "return a new Meeting if authorized and valid" ignore {
      val response = await {
        WS.url(s"http://localhost:$port/meetings").
          withHeaders(("Authorization" -> accessToken.guid.toString)).
          post(meetingJson)
      }

      response.status mustBe CREATED
    }

  }

  val accessToken = AccessToken(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    resourceOwner = User(
      guid = UUID.fromString("00000000-1111-1111-1111-111111111111"),
      handle = "khy",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  val mockAccessTokenClient = new MockAccessTokenClient(Seq(accessToken))
  val mockAccountClient = new MockAccountClient(Seq(accessToken.resourceOwner))

  AccessTokenClient.setMock(mockAccessTokenClient)
  AccountClient.setMock(mockAccountClient)

}
