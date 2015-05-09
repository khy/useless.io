package io.useless.play.authentication

import java.util.UUID
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import io.useless.accesstoken.{ AccessToken, Scope }
import io.useless.account.User
import io.useless.client.accesstoken.{ AccessTokenClient, MockAccessTokenClient }
import io.useless.test.ImplicitPlayApplication

class AuthorizedSpec
  extends FunSpec
  with    Matchers
  with    ImplicitPlayApplication
{

  val readScope = Scope("read")
  val writeScope = Scope("write")
  val deleteScope = Scope("delete")

  val readAccessToken = AccessToken(
    guid = UUID.randomUUID,
    resourceOwner = User(
      guid = UUID.randomUUID,
      handle = "bob",
      name = None
    ),
    client = None,
    scopes = Seq(readScope)
  )

  val writeAccessToken = AccessToken(
    guid = UUID.randomUUID,
    resourceOwner = User(
      guid = UUID.randomUUID,
      handle = "khy",
      name = None
    ),
    client = None,
    scopes = Seq(readScope, writeScope)
  )

  implicit def mockClient = new MockAccessTokenClient(Seq(readAccessToken, writeAccessToken))

  object TestAuthorized extends Authorized("accessTokenGuid", Seq(writeScope, deleteScope)) {
    override lazy val authDao = new ClientAuthDao(UUID.randomUUID)
  }

  object TestController
    extends Controller
  {

    def index = TestAuthorized { request =>
      request.accessToken.resourceOwner match {
        case user: User => Ok("Hi, " + user.handle)
        case _ => Ok("Say, hey")
      }
    }

  }

  describe ("Authorized") {

    it ("should reject an unathenticated request") {
      AccessTokenClient.withMock {
        val result = TestController.index()(FakeRequest())
        status(result) should be (UNAUTHORIZED)
      }
    }

    it ("should reject an authenticated request with insufficient scope") {
      AccessTokenClient.withMock {
        val request = FakeRequest().
          withHeaders(("Authorization" -> readAccessToken.guid.toString))

        val result = TestController.index()(request)
        status(result) should be (UNAUTHORIZED)
      }
    }

    it ("should authorize a request with sufficient scope") {
      AccessTokenClient.withMock {
        val request = FakeRequest().
          withHeaders(("Authorization" -> writeAccessToken.guid.toString))

        val result = TestController.index()(request)
        status(result) should be (OK)
        contentAsString(result) should be ("Hi, khy")
      }
    }

  }

}
