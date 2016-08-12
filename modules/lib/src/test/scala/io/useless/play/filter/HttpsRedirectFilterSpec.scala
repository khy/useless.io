package io.useless.play.filter

import scala.concurrent.Future
import org.scalatest._
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test._
import play.api.mvc._

class HttpsRedirectFilterSpec extends PlaySpec with OneAppPerTest {

  override def newAppForTest(td: TestData): FakeApplication = {
    val global = new WithFilters(new HttpsRedirectFilter) {}
    new FakeApplication(
      withGlobal = Some(global),
      additionalConfiguration = Map(
        "application.router" -> "io.useless.play.filter.EmptyRouter",
        "application.protocol" -> "http"
      ),
      withRoutes = {
        case ("GET", "/path") => Action { Results.Ok }
      }
    )
  }

  val baseRequest = FakeRequest("GET", "/path")

  "A request without 'X-Forwarded-Proto' set" should {

    val request = baseRequest

    "respond 200 if the configured protocol is http" in {
      val result = route(request).get
      status(result) mustBe OK
    }

    "respond 200 if the configured protocol is https" in {
      val result = route(request).get
      status(result) mustBe OK
    }

  }

  "A request with 'X-Forwarded-Proto' set to http" should {

    val request = baseRequest.withHeaders("X-Forwarded-Proto" -> "http")

    "respond 200 if the configured protocol is http" in {
      val result = route(request).get
      status(result) mustBe OK
    }

    "respond 302 if the configured protocol is https" in  {
      val result = route(request).get
      status(result) mustBe MOVED_PERMANENTLY
      redirectLocation(result) mustBe Some("https:///path")
    }

  }

  "A request with 'X-Forwarded-Proto' set to https" should {

    val request = baseRequest.withHeaders("X-Forwarded-Proto" -> "https")

    "respond 302 if the configured protocol is http" in {
      val result = route(request).get
      status(result) mustBe MOVED_PERMANENTLY
      redirectLocation(result) mustBe Some("http:///path")
    }

    "respond 200 if the configured protocol is https" in {
      val result = route(request).get
      status(result) mustBe OK
    }

  }

}
