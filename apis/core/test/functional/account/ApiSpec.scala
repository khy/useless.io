package functional.account

import java.util.UUID
import org.specs2.mutable.Specification
import play.api.test.WithServer
import play.api.test.Helpers._
import play.api.libs.json.Json
import io.useless.util.mongo.MongoUtil

import models.core.account.{ Account, Scope }
import support._

class ApiSpec
  extends Specification
  with    AccountFactory
  with    RequestHelpers
{

  trait Context extends WithServer {
    MongoHelper.clearDb()
    val adminUser = createUser("khy@useless.io", "khy", None, Seq(Scope.Platform))
    val adminAccessToken = adminUser.accessTokens(0).guid.toString
    val url = s"http://localhost:$port/apis"
  }

  "POST /apis" should {
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

    "reject the request if a key is not specified" in new Context {
      val response = post(url, auth = adminAccessToken, body = Json.obj())
      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
    }

    "return successfully if a key is specified, and the request is authenticated with an admin access token" in new Context {
      val response = post(url, auth = adminAccessToken, body = Json.obj("key" -> "haiku"))
      response.status must beEqualTo(CREATED)

      val user = block { Account.forApiKey("haiku") }
      user must beSome
    }

    "reject the request if the specified key already exists" in new Context {
      val body = Json.obj("key" -> "haiku")
      post(url, auth = adminAccessToken, body)
      val response = post(url, auth = adminAccessToken, body)

      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
      response.body must beEqualTo("API account with key 'haiku' already exists.")
    }
  }
}
