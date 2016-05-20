package functional.social

import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import slick.driver.PostgresDriver.api._

import db.core.social._
import models.core.account.Account
import models.core.social.Like
import models.core.social.JsonImplicits._
import support._

class LikeSpec
  extends PlaySpec
  with    OneServerPerSuite
  with    BeforeAndAfterEach
  with    AccountFactory
  with    RequestHelpers
{

  val user = createUser("bob@useless.io", "bob", None)
  override implicit lazy val app = appWithRoute

  val database = Database.forConfig("db.core")

  override def beforeEach {
    await { database.run(Likes.delete) }
  }

  val collectionUrl = s"http://localhost:$port/social/likes"

  "GET /social/likes" should {

    "return a 401 Unauthorized if the request is not authenticated" in {
      val response = get(collectionUrl, auth = None)
      response.status mustBe UNAUTHORIZED
    }

    "return a 200 with any likes for the specified resource" in {
      val like1 = createLike()
      val like2 = createLike(user = createUser("dave@useless.io", "dave", None))

      val response = get(collectionUrl, auth = user.accessTokens(0).guid,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "123"
      )
      response.status mustBe OK

      val likes = response.json.as[Seq[Like]]
      likes.map(_.guid) must contain theSameElementsAs Seq(like1.guid, like2.guid)
    }

    "support pagination" in {
      val like1 = createLike()
      val like2 = createLike(user = createUser("dave@useless.io", "dave", None))
      val like3 = createLike(user = createUser("bill@useless.io", "bill", None))

      val response = get(collectionUrl, auth = user.accessTokens(0).guid,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "123",
        "p.limit" -> "2"
      )

      val likes = response.json.as[Seq[Like]]
      likes.map(_.guid) mustBe Seq(like3.guid, like2.guid)
    }

  }

  val resourceUrl = s"http://localhost:$port/social/likes/beer/bottles/123"

  "PUT /social/likes/:resourceApi/:resourceType/:resourceId" should {

    "return a 401 Unauthorized if the request is not authenticated" in {
      val response = put(resourceUrl, auth = None, body = Json.obj())
      response.status mustBe UNAUTHORIZED
    }

    "return a 201 Created if the request is authorized" in {
      val response = put(resourceUrl, auth = user.accessTokens(0).guid, body = Json.obj())
      response.status mustBe CREATED
    }

    "return a 201 Created with the original like record, if one already exists" in {
      val response1 = put(resourceUrl, auth = user.accessTokens(0).guid, body = Json.obj())
      val like1 = response1.json.as[Like]

      val response2 = put(resourceUrl, auth = user.accessTokens(0).guid, body = Json.obj())
      response2.status mustBe CREATED

      val like2 = response2.json.as[Like]
      like2.guid mustBe like1.guid
    }

  }

  "DELETE /social/likes/:resourceApi/:resourceType/:resourceId" should {

    "return a 401 Unauthorized if the request is not authenticated" in {
      val response = delete(resourceUrl, auth = None)
      response.status mustBe UNAUTHORIZED
    }

    "return a 404 Not Found if the specified like does not exist" in {
      val response = delete(resourceUrl, auth = user.accessTokens(0).guid)
      response.status mustBe NOT_FOUND
    }

    "return a 200 OK if the specified like exists" in {
      val like = createLike()
      val deleteResponse = delete(resourceUrl, auth = user.accessTokens(0).guid)
      deleteResponse.status mustBe OK

      val getResponse = get(collectionUrl, auth = user.accessTokens(0).guid,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "123"
      )
      getResponse.json.as[Seq[Like]].length mustBe 0
    }

  }

  def createLike(
    resourceApi: String = "beer",
    resourceType: String = "bottles",
    resourceId: String = "123",
    user: Account = user
  ): Like = {
    val url = s"http://localhost:$port/social/likes/$resourceApi/$resourceType/$resourceId"
    val response = put(url, auth = user.accessTokens(0).guid, body = Json.obj())
    response.json.as[Like]
  }

}
