package functional.account

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.Play.current
import play.api.test.Helpers._
import play.api.libs.json.Json
import io.useless.util.mongo.MongoUtil

import models.core.account.{ Account, Scope }
import support._

class AppSpec
  extends PlaySpec
  with    OneServerPerSuite
  with    BeforeAndAfterEach
  with    AccountFactory
  with    RequestHelpers
{

  override implicit lazy val app = appWithRoute

  val adminUser = createUser("khy@useless.io", "khy", None, Seq(Scope.Platform))
  val adminAccessToken = adminUser.accessTokens(0).guid
  val url = s"http://localhost:$port/apps"

  "POST /apps" should {
    "reject the request if it is not authenticated" in {
      val response = post(url, auth = None, body = Json.obj())
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in {
      val response = post(url, auth = UUID.randomUUID, body = Json.obj())
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-super user access token" in {
      val user = createUser("bob@useless.io", "bob", None)
      val response = post(url, auth = user.accessTokens(0).guid, body = Json.obj())
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request if a name is not specified" in {
      val body = Json.obj("url" -> "granmal.com", "auth_redirect_url" -> "granmal.com/auth")
      val response = post(url, auth = adminAccessToken, body)
      response.status mustBe(UNPROCESSABLE_ENTITY)
    }

    "reject the request if a url is not specified" in {
      val body = Json.obj("name" -> "Gran Mal", "auth_redirect_url" -> "granmal.com/auth")
      val response = post(url, auth = adminAccessToken, body)
      response.status mustBe(UNPROCESSABLE_ENTITY)
    }

    "reject the request if an auth redirect URL is not specified" in {
      val body = Json.obj("name" -> "Gran Mal", "url" -> "granmal.com")
      val response = post(url, auth = adminAccessToken, body)
      response.status mustBe(UNPROCESSABLE_ENTITY)
    }

    "return successfully if all required properties are specified, and the request is authenticated with an super user access token" in {
      val body = Json.obj("name" -> "Gran Mal", "url" -> "granmal.com", "auth_redirect_url" -> "granmal.com/auth")
      val response = post(url, auth = adminAccessToken, body)
      response.status mustBe(CREATED)

      val account = block { Account.forAppName("Gran Mal") }
      account must be ('defined)
    }

    "reject the request if the specified name already exists" in {
      val body = Json.obj("name" -> "Gran Mal", "url" -> "granmal.com", "auth_redirect_url" -> "granmal.com/auth")
      post(url, auth = adminAccessToken, body)
      val response = post(url, auth = adminAccessToken, body)

      response.status mustBe(UNPROCESSABLE_ENTITY)
      response.body mustBe("App account with name 'Gran Mal' already exists.")
    }
  }
}
