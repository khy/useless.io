package functional.social

import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

import support._

class LikeSpec
  extends PlaySpec
  with    OneServerPerSuite
  with    BeforeAndAfterEach
  with    AccountFactory
  with    RequestHelpers
{

  val url = s"http://localhost:$port/social/likes/beer/bottles/123"
  override implicit lazy val app = appWithRoute

  "PUT /social/likes/beer/bottles/123" should {

    "return a 401 Unauthorized if the request is not authenticated" in {
      val response = put(url, auth = None, body = Json.obj())
      response.status mustBe UNAUTHORIZED
    }

    "return a 201 Created if the request is authorized" in {
      val user = createUser("bob@useless.io", "bob", None)
      val response = put(url, auth = user.accessTokens(0).guid, body = Json.obj())
      println(response.body)
      response.status mustBe CREATED
    }

  }

}
