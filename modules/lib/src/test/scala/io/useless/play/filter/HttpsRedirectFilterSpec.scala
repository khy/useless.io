package io.useless.play.filter

import scala.concurrent.Future
import org.specs2.mutable.{ Specification, Before }
import play.api.mvc._
import play.api.mvc.Results.Ok
import play.api.test._

class HttpsRedirectFilterSpec extends PlaySpecification {

  def buildGlobal() = new WithFilters(new HttpsRedirectFilter) {}

  def buildApplication(protocol: String) = new FakeApplication(
    withGlobal = Some(buildGlobal()),
    additionalConfiguration = Map(
      "application.router" -> "io.useless.play.filter.EmptyRouter",
      "application.protocol" -> protocol
    ),
    withRoutes = {
      case ("GET", "/path") => Action { Ok }
    }
  )

  val httpApplication = buildApplication("http")
  val httpsApplication = buildApplication("https")

  val baseRequest = FakeRequest("GET", "/path")

  "A request without 'X-Forwarded-Proto' set" should {

    val request = baseRequest

    "respond 200 if the configured protocol is http" in new WithApplication(httpApplication) {
      val result = route(request).get
      status(result) must equalTo(OK)
    }

    "respond 200 if the configured protocol is https" in new WithApplication(httpsApplication) {
      val result = route(request).get
      status(result) must equalTo(OK)
    }

  }

  "A request with 'X-Forwarded-Proto' set to http" should {

    val request = baseRequest.withHeaders("X-Forwarded-Proto" -> "http")

    "respond 200 if the configured protocol is http" in new WithApplication(httpApplication) {
      val result = route(request).get
      status(result) must equalTo(OK)
    }

    "respond 302 if the configured protocol is https" in new WithApplication(httpsApplication) {
      val result = route(request).get
      status(result) must equalTo(MOVED_PERMANENTLY)
      redirectLocation(result) must beEqualTo(Some("https:///path"))
    }

  }

  "A request with 'X-Forwarded-Proto' set to https" should {

    val request = baseRequest.withHeaders("X-Forwarded-Proto" -> "https")

    "respond 302 if the configured protocol is http" in new WithApplication(httpApplication) {
      val result = route(request).get
      status(result) must equalTo(MOVED_PERMANENTLY)
      redirectLocation(result) must beEqualTo(Some("http:///path"))
    }

    "respond 200 if the configured protocol is https" in new WithApplication(httpsApplication) {
      val result = route(request).get
      status(result) must equalTo(OK)
    }

  }

}
