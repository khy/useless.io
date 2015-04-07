package functional.account

import org.specs2.mutable.Specification
import play.api.test.WithServer
import play.api.test.Helpers._
import play.api.libs.json.Json
import io.useless.util.mongo.MongoUtil

import models.account.Account
import support.RequestHelpers

class UserSpec
  extends Specification
  with    RequestHelpers
{

  trait Context extends WithServer {
    MongoUtil.clearDb()
    val url = s"http://localhost:$port/users"
  }

  "POST /users" should {
    "reject the request if an email is not specified" in new Context {
      val response = post(url, auth = None, body = Json.obj())
      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
    }

    "reject the request if a handle is not specified" in new Context {
      val response = post(url, auth = None, body = Json.obj("email" -> "khy@useless.io"))
      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
    }

    "return successfully if an email and a handle is specified" in new Context {
      val response = post(url, auth = None, body = Json.obj("email" -> "khy@useless.io", "handle" -> "khy"))
      response.status must beEqualTo(CREATED)

      val user = block { Account.forEmail("khy@useless.io") }
      user must beSome
    }

    "reject the request if the specified email already exists" in new Context {
      val body = Json.obj("email" -> "khy@useless.io", "handle" -> "khy")
      post(url, auth = None, body)
      val response = post(url, auth = None, body)

      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
      response.body must beEqualTo("User account with email khy@useless.io already exists.")
    }

    "reject the request if the specified email is invalid" in new Context {
      val response = post(url, auth = None, body = Json.obj("email" -> "khy@useless", "handle" -> "khy"))

      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
      response.body must beEqualTo("'khy@useless' is not a valid email.")
    }

    "reject the request if the specified handle already exists" in new Context {
      post(url, auth = None, body = Json.obj("email" -> "khy@useless.io", "handle" -> "khy"))
      val response = post(url, auth = None, body = Json.obj("email" -> "khyland@useless.io", "handle" -> "khy"))

      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
      response.body must beEqualTo("User account with handle khy already exists.")
    }

    "reject the request if the specified handle is invalid" in new Context {
      val response = post(url, auth = None, body = Json.obj("email" -> "khy@useless.io", "handle" -> "Kevin Hyland"))

      response.status must beEqualTo(UNPROCESSABLE_ENTITY)
      response.body must beEqualTo("'Kevin Hyland' is not a valid handle.")
    }
  }

}
