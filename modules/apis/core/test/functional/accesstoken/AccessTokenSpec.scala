package functional.accesstoken

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.libs.json.Json
import io.useless.accesstoken.{ AccessToken, AuthorizedAccessToken, Scope => UselessScope }
import io.useless.account.{ App, User }
import io.useless.play.json.accesstoken.AccessTokenJson._
import io.useless.util.mongo.MongoUtil

import models.core.account.Scope
import support._

class AccessTokenSpec
  extends PlaySpec
  with    OneServerPerSuite
  with    BeforeAndAfterEach
  with    AccountFactory
  with    RequestHelpers
{

  override implicit lazy val app = appWithRoute

  "GET /access_tokens/[UUID]" should {

    val user = createUser("khy@useless.io", "khy", Some("Kevin Hyland"))
    val _app = createApp("Gran Mal", "granmal.com")
    val userAccessToken = block { user.addAccessToken(Some(_app.guid), Seq(UselessScope("haiku/read"))) }.right.get
    val url = s"http://localhost:$port/access_tokens/${userAccessToken.guid}"

    "reject the request if it is not authenticated" in {
      val response = get(url, auth = None)
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in {
      val response = get(url, auth = UUID.randomUUID)
      response.status mustBe(UNAUTHORIZED)
    }

    "return a 404 if the access token has been deleted" in {
      val api = createApi("haiku")
      val userAccessToken = block { user.addAccessToken(Some(_app.guid), Seq(UselessScope("haiku/read"))) }.right.get
      userAccessToken.delete()
      val url = s"http://localhost:$port/access_tokens/${userAccessToken.guid}"
      val response = get(url, auth = api.accessTokens(0).guid)
      response.status mustBe (NOT_FOUND)
    }

    "return the specified access token" in {
      val api = createApi("haiku")
      val response = get(url, auth = api.accessTokens(0).guid)
      response.status mustBe (OK)

      val json = Json.parse(response.body)
      val accessToken = Json.fromJson[AccessToken](json).get
      accessToken.scopes must contain (UselessScope("haiku/read"))

      val resourceOwner = accessToken.resourceOwner.asInstanceOf[User]
      resourceOwner.guid mustBe (user.guid)
      resourceOwner.handle mustBe ("khy")
      resourceOwner.name mustBe (Some("Kevin Hyland"))

      val client = accessToken.client.asInstanceOf[Some[App]].get
      client.guid mustBe (_app.guid)
      client.name mustBe ("Gran Mal")
      client.url mustBe ("granmal.com")
    }

  }

  "GET /access_tokens" should {

    val user = createUser("khy@useless.io", "khy", None)
    val adminApp = createApp("Admin", "admin.useless.io")
    val regularApp = createApp("Gran Mal", "granmal.com")

    val adminAccessToken = block { user.addAccessToken(Some(adminApp.guid), Seq(Scope.Admin)) }.right.get
    val regularAccessToken = block { user.addAccessToken(Some(regularApp.guid), Seq()) }.right.get

    val url = s"http://localhost:$port/access_tokens"

    "reject the request if it is not authenticated" in {
      val response = get(url, auth = None)
      response.status mustBe (UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in {
      val response = get(url, auth = UUID.randomUUID)
      response.status mustBe (UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-admin access token" in {
      val response = get(url, auth = regularAccessToken.guid)
      response.status mustBe (UNAUTHORIZED)
    }

    "return all access tokens for the authenticated account" in {
      val response = get(url, auth = adminAccessToken.guid)
      response.status mustBe (OK)

      val json = Json.parse(response.body)
      val accessTokens = Json.fromJson[Seq[AccessToken]](json).get.map { accessToken =>
        accessToken.asInstanceOf[AuthorizedAccessToken]
      }
      accessTokens.length must be >= (2)

      val guids = accessTokens.map(_.guid)
      guids must contain (adminAccessToken.guid)
      guids must contain (regularAccessToken.guid)
    }

  }

  "POST /access_tokens" should {

    val user = createUser("khy@useless.io", "khy", None)
    val regularApp = createApp("Gran Mal", "granmal.com")
    val adminApp = createApp("Admin", "admin.useless.io")

    val adminAccessToken = block { user.addAccessToken(Some(adminApp.guid), Seq(Scope.Admin)) }.right.get
    val regularAccessToken = block { user.addAccessToken(Some(regularApp.guid), Seq()) }.right.get

    val url = s"http://localhost:$port/access_tokens"

    "reject the request if it is not authenticated" in {
      val response = post(url, auth = None, body = Json.obj())
      response.status mustBe (UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in {
      val response = post(url, auth = UUID.randomUUID, body = Json.obj())
      response.status mustBe (UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-admin access token" in {
      val response = post(url, auth = regularAccessToken.guid, body = Json.obj())
      response.status mustBe (UNAUTHORIZED)
    }

    "reject the request if it specifies Platform scope" in {
      val body = Json.obj("scopes" -> Json.arr("platform"))
      val response = post(url, auth = adminAccessToken.guid, body)
      response.status mustBe (UNPROCESSABLE_ENTITY)
    }

    "reject the request if it specifies Auth scope" in {
      val body = Json.obj("scopes" -> Json.arr("auth"))
      val response = post(url, auth = adminAccessToken.guid, body)
      response.status mustBe (UNPROCESSABLE_ENTITY)
    }

    "create an access token for the authenticated account without a client if none is specified, and without scopes if none are specified" in {
      val response = post(url, auth = adminAccessToken.guid, Json.obj())
      val json = Json.parse(response.body)
      val accessToken = Json.fromJson[AccessToken](json).get.asInstanceOf[AuthorizedAccessToken]
      accessToken.client mustBe None
      accessToken.scopes mustBe empty
    }

    "create an access token for the authenticated account with the specified client GUID and scopes" in {
      val body = Json.obj(
        "client_guid" -> regularApp.guid.toString,
        "scopes" -> Json.arr("haiku/read", "haiku/write")
      )
      val response = post(url, auth = adminAccessToken.guid, body)
      val json = Json.parse(response.body)
      val accessToken = Json.fromJson[AccessToken](json).get.asInstanceOf[AuthorizedAccessToken]
      accessToken.client.get.guid mustBe (regularApp.guid)
      accessToken.scopes must contain (UselessScope("haiku/read"))
      accessToken.scopes must contain (UselessScope("haiku/write"))
    }

  }

  "POST /accounts/[UUID]/access_tokens" should {

    val user = createUser("khy@useless.io", "khy", None)
    val authApp = createApp("Admin", "auth.useless.io", Seq(Scope.Auth))
    val trustedApp = createApp("Gran Mal", "granmal.com", Seq(Scope.Trusted))
    val url = s"http://localhost:$port/accounts/${user.guid}/access_tokens"

    "reject the request if it is not authenticated" in {
      val response = post(url, auth = None, body = Json.obj())
      response.status mustBe (UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in {
      val response = post(url, auth = UUID.randomUUID, body = Json.obj())
      response.status mustBe (UNAUTHORIZED)
    }

    "reject the request if it is authenticated with an access token without the Auth or Trusted scope" in {
      val regularApp = createApp("Lamps", "lamps.com")
      val response = post(url, auth = regularApp.accessTokens(0).guid, body = Json.obj())
      response.status mustBe (UNAUTHORIZED)
    }

    "return a 404 if the specified account GUID does not exist" in {
      val badUrl = s"http://localhost:$port/accounts/${UUID.randomUUID.toString}/access_token"
      val response = post(badUrl, auth = authApp.accessTokens(0).guid, body = Json.obj())
      response.status mustBe (NOT_FOUND)
    }

    "create a new access token with the authenticated account as client, and the specified account as resource owner, " +
    "with a scope of Admin, if the request is authenticated with an access token with Auth scope" in {
      val createResponse = post(url, auth = authApp.accessTokens(0).guid, body = Json.obj())
      createResponse.status mustBe (CREATED)

      val json = Json.parse(createResponse.body)
      val accessToken = Json.fromJson[AccessToken](json).get.asInstanceOf[AuthorizedAccessToken]
      accessToken.resourceOwner.guid mustBe (user.guid)
      accessToken.client.get.guid mustBe (authApp.guid)
      accessToken.scopes must contain (Scope.Admin)
    }

    "ignore any requested scopes if the requesting access token has Auth scope" in {
      val response = post(url, auth = authApp.accessTokens(0).guid, Json.obj(
        "scopes" -> Json.arr("platform", "auth", "trusted", "haiku/write"))
      )

      val json = Json.parse(response.body)
      val accessToken = Json.fromJson[AccessToken](json).get.asInstanceOf[AuthorizedAccessToken]
      accessToken.scopes must contain (Scope.Admin)
      accessToken.scopes must not contain (UselessScope("platform"))
      accessToken.scopes must not contain (UselessScope("auth"))
      accessToken.scopes must not contain (UselessScope("trusted"))
      accessToken.scopes must not contain (UselessScope("haiku/write"))
    }

    "reject the request if it is authenticated with a Trusted access token, but asks for Platform scope" in {
      val body = Json.obj("scopes" -> Json.arr("platform"))
      val response = post(url, auth = trustedApp.accessTokens(0).guid, body)
      response.status mustBe (UNPROCESSABLE_ENTITY)
    }

    "reject the request if it is authenticated with a Trusted access token, but asks for Auth scope" in {
      val body = Json.obj("scopes" -> Json.arr("auth"))
      val response = post(url, auth = trustedApp.accessTokens(0).guid, body)
      response.status mustBe (UNPROCESSABLE_ENTITY)
    }

    "reject the request if it is authenticated with a Trusted access token, but asks for Admin scope" in {
      val body = Json.obj("scopes" -> Json.arr("admin"))
      val response = post(url, auth = trustedApp.accessTokens(0).guid, body)
      response.status mustBe (UNPROCESSABLE_ENTITY)
    }

    "reject the request if it is authenticated with a Trusted access token, but asks for Trusted scope" in {
      val body = Json.obj("scopes" -> Json.arr("trusted"))
      val response = post(url, auth = trustedApp.accessTokens(0).guid, body)
      response.status mustBe (UNPROCESSABLE_ENTITY)
    }

    "create a new access token with the authenticated account as client, and the specified account as resource owner, " +
    "with any requested, non-useless-core scopes, if the request is authenticated with an access token with Trusted Scope" in {
      val body = Json.obj("scopes" -> Json.arr("haiku/read", "haiku/write"))
      val response = post(url, auth = trustedApp.accessTokens(0).guid, body)
      response.status mustBe (CREATED)

      val json = Json.parse(response.body)
      val accessToken = Json.fromJson[AccessToken](json).get.asInstanceOf[AuthorizedAccessToken]
      accessToken.resourceOwner.guid mustBe (user.guid)
      accessToken.client.get.guid mustBe (trustedApp.guid)
      accessToken.scopes must contain (UselessScope("haiku/read"))
      accessToken.scopes must contain (UselessScope("haiku/write"))
    }

  }

}
