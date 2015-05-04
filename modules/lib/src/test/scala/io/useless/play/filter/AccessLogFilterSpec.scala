package io.useless.play.filter

import org.scalatest.FunSpec
import org.scalatest.Matchers
import scala.collection.mutable.Stack
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AccessLogFilterSpec
  extends FunSpec
  with    Matchers
{

  private val mockLog = new Stack[String]

  class TestAccessLogFilter extends AbstractAccessLogFilter {
    val requestTimeHeader = "X-Request-Time"
    def log(entry: String) = mockLog.push(entry)
  }

  object TestController extends Controller {
    def index = Action { request => Ok("Hi!") }
  }

  describe ("AccessLogFilter") {

    it ("should log the request method and path") {
      val action = TestController.index()
      val filteredAction = FilterChain(action, List(new TestAccessLogFilter))
      val request = FakeRequest(GET, "/path/to/resource")
      val result = Await.result(filteredAction(request).run, 5.seconds)

      val entry = mockLog.pop()
      entry should include ("GET")
      entry should include ("/path/to/resource")
    }

  }

}
