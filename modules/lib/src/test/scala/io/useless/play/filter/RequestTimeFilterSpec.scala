package io.useless.play.filter

import org.scalatest.FunSpec
import org.scalatest.Matchers
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class RequestTimeFilterSpec
  extends FunSpec
  with    Matchers
{

  object TestController extends Controller {

    def index = Action { request => Ok("Hi!") }

  }

  describe ("RequestTimeFilter") {

    it ("should add an X-Request-Time header to the response") {
      val action = TestController.index()
      val filteredAction = FilterChain(action, List(new RequestTimeFilter))
      val result = filteredAction(FakeRequest()).run
      headers(result).get("X-Request-Time") should not be (None)
    }

    it ("should add a configurable header to the response") {
      val action = TestController.index()
      val filter = new RequestTimeFilter("X-Timing-Thing")
      val filteredAction = FilterChain(action, List(filter))
      val result = filteredAction(FakeRequest()).run
      headers(result).get("X-Timing-Thing") should not be (None)
    }

  }

}
