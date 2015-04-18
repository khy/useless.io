package functional.account

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

import play.api.libs.json.Json
import io.useless.util.mongo.MongoUtil

import models.core.account.Account
import support.RequestHelpers

class UserSpec
  extends PlaySpec
  with    OneServerPerSuite
  with    BeforeAndAfterEach
  with    RequestHelpers
{

  override def beforeEach {
    MongoUtil.clearDb()
  }

  val url = s"http://localhost:$port/users"

  "POST /users" should {
    "reject the request if an email is not specified" in {
      val response = post(url, auth = None, body = Json.obj())
      response.status mustBe UNPROCESSABLE_ENTITY
    }

    "reject the request if a handle is not specified" in {
      val response = post(url, auth = None, body = Json.obj("email" -> "khy@useless.io"))
      response.status mustBe UNPROCESSABLE_ENTITY
    }

    "return successfully if an email and a handle is specified" in {
      val response = post(url, auth = None, body = Json.obj("email" -> "khy@useless.io", "handle" -> "khy"))
      response.status mustBe CREATED

      val user = block { Account.forEmail("khy@useless.io") }
      user mustBe 'some
    }

    "reject the request if the specified email already exists" in {
      val body = Json.obj("email" -> "khy@useless.io", "handle" -> "khy")
      post(url, auth = None, body)
      val response = post(url, auth = None, body)

      response.status mustBe UNPROCESSABLE_ENTITY
      response.body mustBe "User account with email khy@useless.io already exists."
    }

    "reject the request if the specified email is invalid" in {
      val response = post(url, auth = None, body = Json.obj("email" -> "khy@useless", "handle" -> "khy"))

      response.status mustBe UNPROCESSABLE_ENTITY
      response.body mustBe "'khy@useless' is not a valid email."
    }

    "reject the request if the specified handle already exists" in {
      post(url, auth = None, body = Json.obj("email" -> "khy@useless.io", "handle" -> "khy"))
      val response = post(url, auth = None, body = Json.obj("email" -> "khyland@useless.io", "handle" -> "khy"))

      response.status mustBe UNPROCESSABLE_ENTITY
      response.body mustBe "User account with handle khy already exists."
    }

    "reject the request if the specified handle is invalid" in {
      val response = post(url, auth = None, body = Json.obj("email" -> "khy@useless.io", "handle" -> "Kevin Hyland"))

      response.status mustBe UNPROCESSABLE_ENTITY
      response.body mustBe "'Kevin Hyland' is not a valid handle."
    }
  }

}
