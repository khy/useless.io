package functional.account

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.libs.json.Json
import io.useless.account.{ Account, AuthorizedApp, User, AuthorizedUser }
import io.useless.play.json.account.AccountJson._
import io.useless.util.mongo.MongoUtil

import models.core.account.Scope
import support._

class AccountSpec
  extends PlaySpec
  with    OneServerPerSuite
  with    AccountFactory
  with    RequestHelpers
{

  override implicit lazy val app = appWithRoute

  MongoHelper.clearDb()
  val user = createUser("khy@useless.io", "khy", Some("Kevin Hyland"))

  "GET /accounts/[UUID]" should {

    val api = createApi("haiku")
    val url = s"http://localhost:$port/accounts/${user.guid}"

    "reject the request if it is not authenticated" in {
      val response = get(url, auth = None)
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in {
      val response = get(url, auth = UUID.randomUUID)
      response.status mustBe(UNAUTHORIZED)
    }

    "return a 404 if the specified account GUID is non-existant" in {
      val _url = s"http://localhost:$port/auth/accounts/${UUID.randomUUID.toString}"
      val response = get(_url, auth = api.accessTokens(0).guid)
      response.status mustBe(NOT_FOUND)
    }

    "return the specified account if the request is authenticated" in {
      val response = get(url, auth = api.accessTokens(0).guid)
      response.status mustBe(OK)

      val json = Json.parse(response.body)
      val _user = Json.fromJson[Account](json).get.asInstanceOf[User]
      _user.guid mustBe(user.guid)
      _user.handle mustBe("khy")
      _user.name mustBe(Some("Kevin Hyland"))
    }

    "return an authorized representation if the request has Auth scope" in {
      val authApp = createApp("Admin", "admin.useless.com", Seq(Scope.Auth))
      val regularApp = createApp("Gran Mal", "granmal.com")

      val _url = s"http://localhost:$port/accounts/${regularApp.guid}"
      val response = get(_url, auth = authApp.accessTokens(0).guid)
      response.status mustBe(OK)

      val json = Json.parse(response.body)
      val _app = Json.fromJson[Account](json).get.asInstanceOf[AuthorizedApp]
      _app.authRedirectUrl mustBe("granmal.com/auth")
    }

    "return an authorized representation of the account belongs to the requestor, and the request has Admin scope" in {
      val accountApp = createApp("Account", "account.useless.com", Seq.empty)
      val accessToken = block { user.addAccessToken(Some(accountApp.guid), Seq(Scope.Admin)) }.right.get

      val response = get(url, auth = accessToken.guid)
      response.status mustBe(OK)

      val json = Json.parse(response.body)
      val _user = Json.fromJson[Account](json).get.asInstanceOf[AuthorizedUser]
      _user.email mustBe("khy@useless.io")
    }

  }

  "GET /accounts" should {

    val api = createApp("Gran Mal", "granmal.com")
    val appAccessToken = api.accessTokens(0)
    val url = s"http://localhost:$port/accounts"

    "reject the request if it is not authenticated" in {
      val response = get(url, auth = None)
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in {
      val response = get(url, auth = UUID.randomUUID)
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request if no criteria (email, handle, key or name) is specified" in {
      val response = get(url, auth = appAccessToken.guid)
      response.status mustBe(UNPROCESSABLE_ENTITY)
    }

    "return a single account based upon the specified email" in {
      val response = get(url, auth = appAccessToken.guid, query = ("email" -> "khy@useless.io"))
      response.status mustBe(OK)

      val json = Json.parse(response.body)
      val accounts = Json.fromJson[Seq[Account]](json).get
      accounts.length mustBe(1)

      val _user = accounts(0).asInstanceOf[User]
      _user.guid mustBe(user.guid)
      _user.handle mustBe("khy")
      _user.name mustBe(Some("Kevin Hyland"))
    }

    "return no accounts if the specified email is non-existant" in {
      val response = get(url, auth = appAccessToken.guid, query = ("email" -> "nonexistent@useless.io"))
      response.status mustBe(OK)

      val json = Json.parse(response.body)
      val users = Json.fromJson[Seq[Account]](json).get
      users.length mustBe(0)
    }

    "return a single account based upon the specified handle" in {
      val response = get(url, auth = appAccessToken.guid, query = ("handle" -> "khy"))
      response.status mustBe(OK)

      val json = Json.parse(response.body)
      val accounts = Json.fromJson[Seq[Account]](json).get
      accounts.length mustBe(1)

      val _user = accounts(0).asInstanceOf[User]
      _user.guid mustBe(user.guid)
      _user.handle mustBe("khy")
      _user.name mustBe(Some("Kevin Hyland"))
    }

    "return no accounts if the specified handle is non-existant" in {
      val response = get(url, auth = appAccessToken.guid, query = ("handle" -> "bob"))
      response.status mustBe(OK)

      val json = Json.parse(response.body)
      val accounts = Json.fromJson[Seq[Account]](json).get
      accounts.length mustBe(0)
    }

  }

}
