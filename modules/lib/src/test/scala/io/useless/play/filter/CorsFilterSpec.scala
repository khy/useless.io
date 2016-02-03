package io.useless.play.filter

import org.scalatest.{WordSpec, MustMatchers}
import org.scalatestplus.play.OneServerPerSuite
import play.api.mvc._
import play.api.mvc.Results.Ok
import play.api.libs.ws.WS
import play.api.test.FakeApplication
import play.api.test.Helpers._

class CorsFilterSpec
  extends WordSpec
  with MustMatchers
  with OneServerPerSuite
{

  val global = new WithFilters(CorsFilter) {}

  implicit override lazy val app: FakeApplication = FakeApplication(
    withGlobal = Some(global),
    withRoutes = { case ("GET", "/path") => Action { Ok } }
  )

  "CorsRedirectFilter" must {

    "return the appropriate CORS headers for a preflight OPTIONS request" in {
      val response = await { WS.url(s"http://localhost:$port/path").options }
      response.status mustBe OK
      response.header("Access-Control-Allow-Origin") mustBe Some("*")
      response.header("Access-Control-Allow-Methods") mustBe Some("GET,POST,PUT,PATCH,DELETE,OPTIONS")
      response.header("Access-Control-Allow-Headers") mustBe Some("Accept,Authorization,Content-Type")
    }

    "return appropriate CORS headers for regular requests" in {
      val response = await { WS.url(s"http://localhost:$port/path").get }
      response.status mustBe OK
      response.header("Access-Control-Allow-Origin") mustBe Some("*")
      response.header("Access-Control-Allow-Methods") mustBe Some("GET,POST,PUT,PATCH,DELETE,OPTIONS")
      response.header("Access-Control-Allow-Headers") mustBe Some("Accept,Authorization,Content-Type")
    }

  }

}
