package test.functional

import java.util.UUID
import org.scalatestplus.play.{PlaySpec, OneServerPerSuite}
import org.scalatest.BeforeAndAfterEach
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

class HaikuSpec
  extends PlaySpec
  with BeforeAndAfterEach
  with OneServerPerSuite
{

  override def beforeEach {
    MongoUtil.clearDb("haiku.mongo.uri")
  }

  implicit val url = s"http://localhost:$port/haikus"

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

  "POST /haikus" must {

    "reject requests without authentication" in {
      val response = await { WS.url(url).post(Json.obj()) }
      response.status mustBe UNAUTHORIZED
    }

    "reject requests with invalid authentication" in {
      val response = await {
        WS.url(url).
          withHeaders(("Authorization" -> UUID.randomUUID.toString)).
          post(Json.obj())
      }

      response.status mustBe UNAUTHORIZED
    }

    "respond with a 201 Created for an authenticated requests with a valid haiku" in  {
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

      response.status mustBe CREATED
    }

    "respond with a 422 Unprocessable Entity for an authenticated request with an invalid haiku" in {
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

      response.status mustBe UNPROCESSABLE_ENTITY
      (response.json)(0).as[String] mustBe "useless.haiku.error.too_few_syllables"
      (response.json)(1).as[String] mustBe "useless.haiku.error.too_many_syllables"
      (response.json)(2) mustBe JsNull
    }
  }

  "GET /haikus" must {

    "accept requests without authentication" in {
      val response = await { WS.url(url).get() }
      response.status mustBe OK
    }

    "accept requests with invalid authentication" in {
      val response = await {
        WS.url(url).
          withHeaders(("Authorization" -> UUID.randomUUID.toString)).
          get()
      }

      response.status mustBe OK
    }

    "return a list of haikus, most recent first" in {
      createHaiku1
      createHaiku2

      val response = await { WS.url(url).get() }
      val haikus = Json.parse(response.body).as[Seq[JsValue]]

      haikus.size mustBe 2
      (haikus(0) \ "lines")(0).as[String] mustBe "even a horse"
      (haikus(0) \ "createdBy" \ "handle").as[String] mustBe "khy"
      (haikus(1) \ "lines")(0).as[String] mustBe "by my new banana plant"
      (haikus(1) \ "createdBy" \ "handle").as[String] mustBe "khy"
    }

    "return haikus that are after the specified 'p.after' paramter" in {
      val createResponse1 = createHaiku1
      val createResponse2 = createHaiku2
      val latestHaiku = Json.parse(createResponse2.body)
      val latestGuid = (latestHaiku \ "guid").as[String]
      val nextLatestHaiku = Json.parse(createResponse1.body)
      val nextLatestGuid = (nextLatestHaiku \ "guid").as[String]

      val response = await { WS.url(url).withQueryString("p.after" -> latestGuid).get() }
      response.header("Link").get must endWith ("rel=\"next\"")
      val haikus = Json.parse(response.body).as[Seq[JsValue]]

      haikus.size mustBe 1
      (haikus(0) \ "guid").as[String] mustBe nextLatestGuid
    }

    "return haikus for the specified page" in {
      val nextLatestHaiku = Json.parse(createHaiku1.body)
      val latestHaiku = Json.parse(createHaiku2.body)

      val response = await { WS.url(url).withQueryString("p.page" -> "2", "p.limit" -> "1").get() }
      val haikus = Json.parse(response.body).as[Seq[JsValue]]

      haikus.size mustBe 1
      (haikus(0) \ "guid").as[String] mustBe (nextLatestHaiku \ "guid").as[String]
    }

    "return haikus after the specified offset" in {
      val nextLatestHaiku = Json.parse(createHaiku1.body)
      val latestHaiku = Json.parse(createHaiku2.body)

      val response = await { WS.url(url).withQueryString("p.offset" -> "1", "p.limit" -> "1").get() }
      val haikus = Json.parse(response.body).as[Seq[JsValue]]

      haikus.size mustBe 1
      (haikus(0) \ "guid").as[String] mustBe (nextLatestHaiku \ "guid").as[String]
    }

    "return haikus that belong to the user specified by the 'user' parameter" in {
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

      haikus.size mustBe 2
      (haikus(0) \ "createdBy" \ "handle").as[String] mustBe "khy"
      (haikus(1) \ "createdBy" \ "handle").as[String] mustBe "khy"
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
