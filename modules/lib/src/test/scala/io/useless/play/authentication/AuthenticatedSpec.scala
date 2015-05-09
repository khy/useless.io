package io.useless.play.authentication

import java.util.UUID
import scala.concurrent.Future

import org.scalatest.FunSpec
import org.scalatest.Matchers

import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import io.useless.accesstoken.AccessToken
import io.useless.account.{ Account, User }
import io.useless.client.UnauthorizedException
import io.useless.client.accesstoken.{ AccessTokenClient, MockAccessTokenClient }
import io.useless.test.ImplicitPlayApplication

class AuthenticationSpec
  extends FunSpec
  with    Matchers
  with    ImplicitPlayApplication
{



  val accessToken = AccessToken(
    guid = UUID.fromString("3a65a664-89a0-4f5b-8b9e-f3226af0ff99"),
    resourceOwner = User(
      guid = UUID.randomUUID,
      handle = "khy",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  implicit def mockClient = new MockAccessTokenClient(Seq(accessToken))

  object TestAuthenticated extends Authenticated("accessTokenGuid") {
    override lazy val authDao = new ClientAuthDao(UUID.randomUUID)
  }

  object TestController
    extends Controller
  {

    def index = TestAuthenticated { request =>
      request.accessToken.resourceOwner match {
        case user: User => Ok("Hi, " + user.handle)
        case _ => Ok("Say, hey")
      }
    }

  }

  describe ("Authenticated") {

    it ("should reject an unathenticated request") {
      AccessTokenClient.withMock {
        val result = TestController.index()(FakeRequest())
        status(result) should be (UNAUTHORIZED)
      }
    }

    it ("should authenticate a request with a valid Authorization header") {
      AccessTokenClient.withMock {
        val request = FakeRequest().
          withHeaders(("Authorization" -> "3a65a664-89a0-4f5b-8b9e-f3226af0ff99"))

        val result = TestController.index()(request)
        status(result) should be (OK)
        contentAsString(result) should be ("Hi, khy")
      }
    }

    it ("should authenticate a request with a valid 'auth' query parameter") {
      AccessTokenClient.withMock {
        val request = FakeRequest(GET, "http://some-api.useless.io/index?auth=3a65a664-89a0-4f5b-8b9e-f3226af0ff99")
        val result = TestController.index()(request)
        status(result) should be (OK)
        contentAsString(result) should be ("Hi, khy")
      }
    }

    it ("should authenticate a request with a valid 'auth' cookie") {
      AccessTokenClient.withMock {
        val request = FakeRequest().
          withCookies(Cookie("auth", "3a65a664-89a0-4f5b-8b9e-f3226af0ff99"))

        val result = TestController.index()(request)
        status(result) should be (OK)
        contentAsString(result) should be ("Hi, khy")
      }
    }

    ignore ("should throw an error if any auth attempt fails") {
      val mockFailureClient = new AccessTokenClient {
        def getAccessToken(guid: UUID) = {
          Future.failed(new UnauthorizedException("auth"))
        }
      }

      AccessTokenClient.withMock(mockFailureClient) {
        val request = FakeRequest().
          withHeaders(("Authorization" -> "3a65a664-89a0-4f5b-8b9e-f3226af0ff99"))

        val result = TestController.index()(request)
        a [UnauthorizedException] should be thrownBy { status(result) }
      }
    }

  }

}
