package functional.account

import java.util.UUID
import org.specs2.mutable.Specification
import play.api.test.WithServer
import play.api.test.Helpers._
import play.api.libs.json.Json
import io.useless.util.mongo.MongoUtil

import models.account.{ Account, Scope }
import support.{ AccountFactory, RequestHelpers }

class AppSpec
  extends Specification
  with    AccountFactory
  with    RequestHelpers
{

  trait Context extends WithServer {
    MongoUtil.clearDb()
    val adminUser = createUser("khy@useless.io", "khy", None, Seq(Scope.Platform))
    val adminAccessToken = adminUser.accessTokens(0).guid
    val url = s"http://localhost:$port/apps"
  }

  "POST /apps" should {
    "reject the request if it is not authenticated" in new Context {
      val response = post(url, auth = None, body = Json.obj())
      response.status must beEqualTo(UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in new Context {
      val response = post(url, auth = UUID.randomUUID, body = Json.obj())
      response.status must beEqualTo(UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-super user access token" in new Context {
      val user = createUser("bob@useless.io", "bob", None)
      val response = post(url, auth = user.accessTokens(0).guid, body = Json.obj())
      response.status must beEqualTo(UNAUTHORIZED)
    }

    "reject the request if a name is not specified" in new Context {
      val body = Json.obj("url" -> "granmal.com", "auth_redirect_url" -> "granmal.com/auth")
      val response = post(url, auth = adminAccessToken, body)
      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
    }

    "reject the request if a url is not specified" in new Context {
      val body = Json.obj("name" -> "Gran Mal", "auth_redirect_url" -> "granmal.com/auth")
      val response = post(url, auth = adminAccessToken, body)
      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
    }

    "reject the request if an auth redirect URL is not specified" in new Context {
      val body = Json.obj("name" -> "Gran Mal", "url" -> "granmal.com")
      val response = post(url, auth = adminAccessToken, body)
      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
    }

    "return successfully if all required properties are specified, and the request is authenticated with an super user access token" in new Context {
      val body = Json.obj("name" -> "Gran Mal", "url" -> "granmal.com", "auth_redirect_url" -> "granmal.com/auth")
      val response = post(url, auth = adminAccessToken, body)
      response.status must beEqualTo(CREATED)

      val account = block { Account.forAppName("Gran Mal") }
      account must beSome
    }

    "reject the request if the specified name already exists" in new Context {
      val body = Json.obj("name" -> "Gran Mal", "url" -> "granmal.com", "auth_redirect_url" -> "granmal.com/auth")
      post(url, auth = adminAccessToken, body)
      val response = post(url, auth = adminAccessToken, body)

      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
      response.body must beEqualTo("App account with name 'Gran Mal' already exists.")
    }
  }
}
