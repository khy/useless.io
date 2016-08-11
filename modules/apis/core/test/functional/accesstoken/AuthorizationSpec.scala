package functional.accesstoken

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.libs.json.Json
import io.useless.accesstoken.AccessToken
import io.useless.util.mongo.MongoUtil
import io.useless.play.json.UuidJson._
import io.useless.play.json.accesstoken.AccessTokenJson._

import support._

class AuthorizationSpec
  extends PlaySpec
  with    OneServerPerSuite
  with    BeforeAndAfterEach
  with    AccountFactory
  with    RequestHelpers
{

  override implicit lazy val app = appWithRoute

  "POST /access_tokens/authorizations" should {

    val _app = createApp("Account", "account.useless.io")
    val user = createUser("khy@useless.io", "khy", None)
    val accessToken = block { user.addAccessToken(Some(_app.guid), Seq()) }.right.get
    val url = s"http://localhost:$port/access_tokens/authorizations/${accessToken.authorizationCode}"

    "reject the request if it is not authenticated" in {
      val response = post(url, auth = None, Json.obj())
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request if it is authenticated with a non-existant access token" in {
      val response = post(url, auth = UUID.randomUUID, Json.obj())
      response.status mustBe(UNAUTHORIZED)
    }

    "reject the request for a non-existant account GUID" in {
      val badUrl = s"http://localhost:$port/access_tokens/authorizations/${UUID.randomUUID}"
      val response = post(badUrl, auth = _app.accessTokens(0).guid, Json.obj())
      response.status mustBe(UNPROCESSABLE_ENTITY)
    }

    "reject the request if the access token referenced by the authorization code does not belong to the requestor" in {
      val otherApp = createApp("Gran Mal", "granmal.com")
      val response = post(url, auth = otherApp.accessTokens(0).guid, Json.obj())
      response.status mustBe(UNPROCESSABLE_ENTITY)
    }

    "return the access token associated with the specified authorization code if it belongs to the requestor" in {
      val response = post(url, auth = _app.accessTokens(0).guid, Json.obj())
      response.status mustBe(CREATED)

      val json = Json.parse(response.body)
      val _accessToken = Json.fromJson[AccessToken](json).get
      _accessToken.guid mustBe(accessToken.guid)
    }

    "reject the request if the authorization code has been already authorized, and the access token should be deleted" in {
      post(url, auth = _app.accessTokens(0).guid, Json.obj())
      val response = post(url, auth = _app.accessTokens(0).guid, Json.obj())
      response.status mustBe(UNPROCESSABLE_ENTITY)
      val _accessToken = block { user.reload() }.accessTokens.find(_.guid == accessToken.guid).get
      _accessToken mustBe 'deleted
    }

    "reject the request if it is made more than 10 minutes after the access token was created, and the access token should be deleted" in {
      import org.joda.time.DateTime
      import scala.concurrent.ExecutionContext.Implicits.global
      import reactivemongo.bson._
      import io.useless.reactivemongo.bson.UuidBson._
      import io.useless.reactivemongo.bson.DateTimeBson._

      block { accountCollection.update(
        BSONDocument("_id" -> user.guid, "access_tokens.guid" -> accessToken.guid),
        BSONDocument("$set" ->
          BSONDocument("access_tokens.$.created_at" -> DateTime.now.minusMinutes(11))
        )
      ) }

      val response = post(url, auth = _app.accessTokens(0).guid, Json.obj())
      response.status mustBe(UNPROCESSABLE_ENTITY)
      val _accessToken = block { user.reload() }.accessTokens.find(_.guid == accessToken.guid).get
      _accessToken mustBe 'deleted
    }

    "reject the request if the authorization code has been deleted" in {
      accessToken.delete()
      val response = post(url, auth = _app.accessTokens(0).guid, Json.obj())
      response.status mustBe(UNPROCESSABLE_ENTITY)
    }

  }

}
