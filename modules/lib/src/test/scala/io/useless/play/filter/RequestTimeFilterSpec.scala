package io.useless.play.filter

import org.scalatest.FunSpec
import org.scalatest.Matchers
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class RequestTimeFilterSpec
  extends FunSpec
  with    Matchers
{

  describe ("RequestTimeFilter") {

    it ("should add an X-Request-Time header to the response") {
      val action = Action { Results.Ok("Hi!") }
      val filter = new RequestTimeFilter
      val result = filter(action)(FakeRequest()).run
      headers(result).get("X-Request-Time") should not be (None)
    }

    it ("should add a configurable header to the response") {
      val action = Action { Results.Ok("Hi!") }
      val filter = new RequestTimeFilter("X-Timing-Thing")
      val result = filter(action)(FakeRequest()).run
      headers(result).get("X-Timing-Thing") should not be (None)
    }

  }

}
