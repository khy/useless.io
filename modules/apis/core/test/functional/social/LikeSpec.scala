package functional.social

import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import slick.driver.PostgresDriver.api._
import io.useless.http.LinkHeader
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._

import db.core.social._
import models.core.account.Account
import models.core.social.{Like, LikeAggregate}
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

    "return a 200 with any likes for the specified resource" in {
      val like1 = createLike()
      val like2 = createLike(user = createUser("dave@useless.io", "dave", None))

      val response = get(collectionUrl, auth = None,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "123"
      )
      response.status mustBe OK

      val likes = response.json.as[Seq[Like]]
      likes.map(_.guid) must contain theSameElementsAs Seq(like1.guid, like2.guid)
    }

    "support specifying multiples of an individual parameter" in {
      val like1 = createLike()
      val like2 = createLike(resourceId = "456")

      val response = get(collectionUrl, auth = None,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "123",
        "resourceId" -> "456"
      )
      response.status mustBe OK

      val likes = response.json.as[Seq[Like]]
      likes.map(_.guid) must contain theSameElementsAs Seq(like1.guid, like2.guid)
    }

    "support specifying an account guid" in {
      val user = createUser("dave@useless.io", "dave", None)
      val like1 = createLike()
      val like2 = createLike(user = user)

      val response = get(collectionUrl, auth = None,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "123",
        "accountGuid" -> user.guid.toString
      )
      response.status mustBe OK

      val likes = response.json.as[Seq[Like]]
      likes.map(_.guid) must contain theSameElementsAs Seq(like2.guid)
    }

    "support pagination" in {
      val like1 = createLike()
      val like2 = createLike(user = createUser("dave@useless.io", "dave", None))
      val like3 = createLike(user = createUser("bill@useless.io", "bill", None))

      val response = get(collectionUrl, auth = None,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "123",
        "p.limit" -> "2"
      )

      val likes = response.json.as[Seq[Like]]
      likes.map(_.guid) mustBe Seq(like3.guid, like2.guid)
    }

  }

  val aggregatesUrl = s"http://localhost:$port/social/likes/aggregates"

  "GET /social/likes/aggregates" should {

    "return a 200 with any likes for the specified resource type" in {
      val user1 = createUser("dave@useless.io", "dave", None)
      val user2 = createUser("bill@useless.io", "bill", None)
      createLike("beer", "bottles", "123", user1)
      createLike("beer", "bottles", "123", user2)
      createLike("beer", "bottles", "456", user1)
      createLike("beer", "breweries", "123", user1)

      val response = get(aggregatesUrl, auth = None,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles"
      )
      response.status mustBe OK

      val likeAggs = response.json.as[Seq[LikeAggregate]]
      likeAggs.length mustBe 2
      likeAggs.find(_.resourceId == "123").get.count mustBe 2
      likeAggs.find(_.resourceId == "456").get.count mustBe 1
    }

    "return only likes for the specified resource IDs" in {
      val user1 = createUser("dave@useless.io", "dave", None)
      val user2 = createUser("bill@useless.io", "bill", None)
      createLike("beer", "bottles", "123", user1)
      createLike("beer", "bottles", "123", user2)
      createLike("beer", "bottles", "456", user1)
      createLike("beer", "bottles", "789", user2)

      val response = get(aggregatesUrl, auth = None,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "123",
        "resourceId" -> "456"
      )
      response.status mustBe OK

      val likeAggs = response.json.as[Seq[LikeAggregate]]
      likeAggs.length mustBe 2
      likeAggs.find(_.resourceId == "123").get.count mustBe 2
      likeAggs.find(_.resourceId == "456").get.count mustBe 1
    }

    "support offset- / page-based pagination, ordering by the api / type / ID concatenated key" in {
      val user1 = createUser("dave@useless.io", "dave", None)
      val user2 = createUser("bill@useless.io", "bill", None)
      createLike("beer", "ingredients", "123", user1)
      createLike("beer", "ingredients", "123", user2)
      createLike("beer", "bottles", "123", user1)
      createLike("beer", "bottles", "123", user2)
      createLike("beer", "bottles", "456", user1)
      createLike("beer", "bottles", "789", user2)

      val response1 = get(aggregatesUrl, auth = None,
        "resourceApi" -> "beer",
        "p.limit" -> "3"
      )
      response1.status mustBe OK

      val likeAggs1 = response1.json.as[Seq[LikeAggregate]]
      likeAggs1.length mustBe 3
      likeAggs1.map(_.resourceType) must not contain ("ingredients")

      val nextUrl = LinkHeader.parse(response1.header("Link").get).find(_.relation == "next").get.url
      val response2 = get(nextUrl, auth = user.accessTokens(0).guid)
      val likeAggs2 = response2.json.as[Seq[LikeAggregate]]
      likeAggs2.length mustBe 1
      likeAggs2.head.resourceType mustBe "ingredients"
      likeAggs2.head.count mustBe 2
    }

    "not support precedence-based pagination" in {
      val response = get(aggregatesUrl, auth = None,
        "resourceApi" -> "beer",
        "p.after" -> "abc"
      )

      response.status mustBe CONFLICT
      val errors = response.json.as[Seq[Errors]]
      errors.head.key mustBe Some("pagination.style")
      errors.head.messages.head.key mustBe "useless.error.invalid-value"
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

    "not return a deleted like record, if one already exists" in {
      val like1 = createLike()
      val deleteResponse = delete(resourceUrl, auth = user.accessTokens(0).guid)

      val likeResponse = put(resourceUrl, auth = user.accessTokens(0).guid, body = Json.obj())
      likeResponse.status mustBe CREATED

      val like2 = likeResponse.json.as[Like]
      like2.guid must not be like1.guid
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

    "only delete the specified like" in {
      createLike(resourceId = "123")
      createLike(resourceId = "456")

      val deleteResponse = delete(resourceUrl, auth = user.accessTokens(0).guid)

      val getResponse = get(collectionUrl, auth = user.accessTokens(0).guid,
        "resourceApi" -> "beer",
        "resourceType" -> "bottles",
        "resourceId" -> "456"
      )
      getResponse.json.as[Seq[Like]].length mustBe 1
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
