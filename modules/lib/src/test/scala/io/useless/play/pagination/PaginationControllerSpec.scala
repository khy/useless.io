package io.useless.play.pagination

import java.util.UUID
import scala.concurrent.Future
import org.scalatest._
import play.api.test.FakeRequest
import play.api.mvc._
import play.api.test.Helpers._

import io.useless.account._
import io.useless.http.LinkHeader
import io.useless.play.json.account.AccountJson._
import io.useless.pagination._

class PaginationControllerSpec
  extends WordSpec
  with MustMatchers
{

  object TestController
    extends Controller
    with PaginationController
  {

    def sync = Action { implicit request =>
      withRawPaginationParams { rawPaginationParams =>
        paginatedResult(call, result(rawPaginationParams))
      }
    }

    def async = Action.async { implicit request =>
      withRawPaginationParams { rawPaginationParams =>
        Future.successful(paginatedResult(call, result(rawPaginationParams)))
      }
    }

    private val call = Call("GET", "test.useless.io/things")

    private def result(rawPaginationParams: RawPaginationParams) = {
      val paginationParams = PaginationParams.build(rawPaginationParams).right.get
      val result = Seq(Api(UUID.randomUUID, "books"), Api(UUID.randomUUID, "haikus"))
      PaginatedResult.build(result, paginationParams, totalItems = Some(200))
    }

  }

  "PaginationController#withRawPaginationParams" should {

    "reject a request with invalid pagination params" in {
      val request = FakeRequest("GET", "/things?p.limit=abc")

      val syncResult = TestController.sync()(request)
      val asyncResult = TestController.async()(request)
      status(syncResult) mustBe (CONFLICT)
      status(asyncResult) mustBe (CONFLICT)
    }

    "accept a request with valid pagination params" in {
      val request = FakeRequest("GET", "/things?p.limit=2")

      val syncResult = TestController.sync()(request)
      val asyncResult = TestController.async()(request)
      status(syncResult) mustBe (OK)
      status(asyncResult) mustBe (OK)
    }

  }

  "PaginationController#paginatedResult" should {

    "return a Link header" in {
      val request = FakeRequest("GET", "/things?p.limit=2&p.offset=20")

      val result = TestController.sync()(request)
      status(result) mustBe (OK)

      val links = header("Link", result).get
      val parsedLinks = LinkHeader.parse(links)

      Seq("first", "previous", "next", "last").foreach { relation =>
        parsedLinks.find { _.relation == relation } mustBe 'defined
      }
    }

    "preserve non-pagination params" in {
      val request = FakeRequest("GET", "/things?name=kevin&p.limit=2&p.offset=20")

      val result = TestController.sync()(request)
      status(result) mustBe (OK)

      val links = header("Link", result).get
      val parsedLinks = LinkHeader.parse(links)

      Seq("first", "previous", "next", "last").foreach { relation =>
        parsedLinks.find { _.relation == relation }.get.url must include ("name=kevin")
      }
    }

  }

}
