package test.functional

import java.util.UUID
import org.specs2.mutable._
import play.api.Application
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WS
import play.api.libs.json.{ Json, JsValue, JsArray, JsNull }
import io.useless.accesstoken.AccessToken
import io.useless.account.User
import io.useless.client.accesstoken.{ AccessTokenClient, MockAccessTokenClient }
import io.useless.client.account.{ AccountClient, MockAccountClient }
import io.useless.util.mongo.MongoUtil

import models.haiku.Haiku
import models.haiku.JsonImplicits._

class HaikuSpec extends PlaySpecification {

  trait Context extends WithServer {
    MongoUtil.clearDb("haiku.mongo.uri")
    implicit val url = s"http://localhost:$port/haikus"
  }

  val khy = AccessToken(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    resourceOwner = User(
      guid = UUID.randomUUID,
      handle = "khy",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  val bob = AccessToken(
    guid = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    resourceOwner = User(
      guid = UUID.randomUUID,
      handle = "bob",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  val mockAccessTokenClient = new MockAccessTokenClient(Seq(khy, bob))
  AccessTokenClient.setMock(mockAccessTokenClient)

  val mockAccountClient = new MockAccountClient(Seq(khy.resourceOwner, bob.resourceOwner))
  AccountClient.setMock(mockAccountClient)

  "POST /haikus" should {

    "reject requests without authentication" in new Context {
      val response = await { WS.url(url).post(Json.obj()) }
      response.status must beEqualTo(UNAUTHORIZED)
    }

    "reject requests with invalid authentication" in new Context {
      val response = await {
        WS.url(url).
          withHeaders(("Authorization" -> UUID.randomUUID.toString)).
          post(Json.obj())
      }

      response.status must beEqualTo(UNAUTHORIZED)
    }

    "respond with a 201 Created for an authenticated requests with a valid haiku" in new Context {
      val response = await { WS.url(url).
        withHeaders(("Authorization" -> "00000000-0000-0000-0000-000000000000")).
        post(Json.obj(
          "lines" -> Json.arr(
            "the Dutchmen, too,",
            "kneel before His Lordship—",
            "spring under His reign"
          )
        ))
      }

      response.status must beEqualTo(CREATED)
    }

    "respond with a 422 Unprocessable Entity for an authenticated request with an invalid haiku" in new Context {
      val response = await { WS.url(url).
        withHeaders(("Authorization" -> "00000000-0000-0000-0000-000000000000")).
        post(Json.obj(
          "lines" -> Json.arr(
            "Yukon Jack",
            "is a taste born of hoary nights, when lonely men struggled to keep their fires",
            "lit and cabins warm"
          )
        ))
      }

      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
      (response.json)(0).as[String] must beEqualTo("useless.haiku.error.too_few_syllables")
      (response.json)(1).as[String] must beEqualTo("useless.haiku.error.too_many_syllables")
      (response.json)(2) must beEqualTo(JsNull)
    }
  }

  "GET /haikus" should {

    "accept requests without authentication" in new Context {
      val response = await { WS.url(url).get() }
      response.status must beEqualTo(OK)
    }

    "accept requests with invalid authentication" in new Context {
      val response = await {
        WS.url(url).
          withHeaders(("Authorization" -> UUID.randomUUID.toString)).
          get()
      }

      response.status must beEqualTo(OK)
    }

    "return a list of haikus, most recent first" in new Context {
      createHaiku1
      createHaiku2

      val response = await { WS.url(url).get() }
      val haikus = Json.parse(response.body).as[Seq[JsValue]]

      haikus.size must beEqualTo(2)
      (haikus(0) \ "lines")(0).as[String] must beEqualTo("even a horse")
      (haikus(0) \ "createdBy" \ "handle").as[String] must beEqualTo("khy")
      (haikus(1) \ "lines")(0).as[String] must beEqualTo("by my new banana plant")
      (haikus(1) \ "createdBy" \ "handle").as[String] must beEqualTo("khy")
    }

    "return haikus that are before the specified 'until' paramter" in new Context {
      val createResponse1 = createHaiku1
      val createResponse2 = createHaiku2
      val latestHaiku = Json.parse(createResponse2.body)
      val latestGuid = (latestHaiku \ "guid").as[String]
      val nextLatestHaiku = Json.parse(createResponse1.body)
      val nextLatestGuid = (nextLatestHaiku \ "guid").as[String]

      val response = await { WS.url(url).withQueryString("until" -> latestGuid).get() }
      val haikus = Json.parse(response.body).as[Seq[JsValue]]

      haikus.size must beEqualTo(1)
      (haikus(0) \ "guid").as[String] must beEqualTo(nextLatestGuid)
    }

    "return haikus that belong to the user specified by the 'user' parameter" in new Context {
      createHaiku1
      createHaiku2
      await { WS.url(url).
        withHeaders(("Authorization" -> "11111111-1111-1111-1111-111111111111")).
        post(Json.obj(
          "lines" -> Json.arr(
            "another year is gone",
            "a traveler's shade on my head,",
            "straw sandals at my feet"
          )
        ))
      }

      val response = await { WS.url(url).withQueryString("user" -> "khy").get() }
      val haikus = Json.parse(response.body).as[Seq[JsValue]]

      haikus.size must beEqualTo(2)
      (haikus(0) \ "createdBy" \ "handle").as[String] must beEqualTo("khy")
      (haikus(1) \ "createdBy" \ "handle").as[String] must beEqualTo("khy")
    }

  }

  def createHaiku1(implicit url: String, app: Application) = await { WS.url(url).
    withHeaders(("Authorization" -> "00000000-0000-0000-0000-000000000000")).
    post(Json.obj(
      "lines" -> Json.arr(
        "by my new banana plant",
        "the first sign of something I loathe—",
        "a miscanthus bud!"
      )
    ))
  }

  def createHaiku2(implicit url: String, app: Application) = await { WS.url(url).
    withHeaders(("Authorization" -> "00000000-0000-0000-0000-000000000000")).
    post(Json.obj(
      "lines" -> Json.arr(
        "even a horse",
        "arrests my eyes—on this",
        "snowy morrow"
      )
    ))
  }

  def createHaiku3(implicit url: String, app: Application) = await { WS.url(url).
    withHeaders(("Authorization" -> "00000000-0000-0000-0000-000000000000")).
    post(Json.obj(
      "lines" -> Json.arr(
        "another year is gone",
        "a traveler's shade on my head,",
        "straw sandals at my feet"
      )
    ))
  }

}
