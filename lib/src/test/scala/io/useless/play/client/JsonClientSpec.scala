package io.useless.play.client

import org.scalatest.FunSpec
import org.scalatest.Matchers
import java.util.UUID
import play.api.libs.json._

import io.useless.client._
import io.useless.account.{ Account, Api }
import io.useless.play.json.account.AccountJson._
import io.useless.test.Await
import io.useless.http.LinkHeader

class JsonClientSpec
  extends FunSpec
  with    Matchers
{

  class ApiJsonClient(
    status: Int,
    json: JsValue = JsNull,
    body: Option[String] = None,
    linkHeader: Option[String] = None
  ) extends JsonClient with MockBaseClientComponent {

    override def baseClient(auth: String) = new MockBaseClient(status, json, body, linkHeader)

    val _jsonClient = jsonClient("auth")

    def get(path: String) = _jsonClient.get(path)

    def find(path: String) = _jsonClient.find(path)

    def create(path: String) = _jsonClient.create(path, JsNull)

  }

  describe ("JsonClient#get") {

    it ("should return None if the baseClient returns a 404") {
      val client = new ApiJsonClient(404, JsNull)
      Await(client.get("/api")) should be (None)
    }

    it ("should throw an UnauthorizedException if the baseClient returns a 401") {
      val client = new ApiJsonClient(401, JsNull)
      a [UnauthorizedException] should be thrownBy { Await(client.get("/api")) }
    }

    it ("should throw a ServerErrorException if the baseClient returns a 500") {
      val client = new ApiJsonClient(500, JsNull)
      a [ServerErrorException] should be thrownBy { Await(client.get("/api")) }
    }

    it ("should throw an UnexpectedStatusException if the baseClient returns something besides a 200, 401, 404 or 500") {
      val client = new ApiJsonClient(302, JsNull)
      a [UnexpectedStatusException] should be thrownBy { Await(client.get("/api")) }
    }

    it ("should return a JSON value") {
      val client = new ApiJsonClient(200, Json.obj("abc" -> 123))
      val response = Await(client.get("/api"))
      (response.get \ "abc").as[Int] should be (123)
    }

  }

  describe ("JsonClient#find") {

    it ("should throw a NonexistentResourceException if the baseClient returns a 404") {
      val client = new ApiJsonClient(404, JsNull)
      a [UnexpectedStatusException] should be thrownBy  { Await(client.find("/jeh")) }
    }

    it ("should throw an UnauthorizedException if the baseClient returns a 401") {
      val client = new ApiJsonClient(401, JsNull)
      a [UnauthorizedException] should be thrownBy { Await(client.find("/api")) }
    }

    it ("should throw a ServerErrorException if the baseClient returns a 500") {
      val client = new ApiJsonClient(500, JsNull)
      a [ServerErrorException] should be thrownBy { Await(client.find("/api")) }
    }

    it ("should throw an UnexpectedStatusException if the baseClient returns something besides a 200, 401, 404 or 500") {
      val client = new ApiJsonClient(302, JsNull)
      a [UnexpectedStatusException] should be thrownBy { Await(client.find("/api")) }
    }

    it ("should return a JSON value") {
      val client = new ApiJsonClient(200, Json.arr(Json.obj("abc" -> 123)))
      val page = Await(client.find("/api"))
      (page.items(0) \ "abc").as[Int] should be (123)
    }

    it ("should return links, if included in the Link header") {
      val linkHeader = LinkHeader.build(Seq(
        LinkHeader.LinkValue("next", "http://useless.io/next")
      ))
      val json = Json.arr(Json.obj("abc" -> 123))
      val client = new ApiJsonClient(200, json, linkHeader = Some(linkHeader))
      val page = Await(client.find("/api"))
      page.next should be (Some("http://useless.io/next"))
    }

  }

  describe ("JsonClient#create") {

    it ("should throw a NonexistentResourceException if the baseClient returns a 404") {
      val client = new ApiJsonClient(404, JsNull)
      a [UnexpectedStatusException] should be thrownBy  { Await(client.create("/api")) }
    }

    it ("should return a Left with the response body if the baseClient returns a 422") {
      val client = new ApiJsonClient(422, body = Some("You did something wrong"))
      Await(client.create("/api")).left.get should be ("You did something wrong")
    }

    it ("should return a Left with the response body if the baseClient returns a 409") {
      val client = new ApiJsonClient(409, body = Some("You did something wrong"))
      Await(client.create("/api")).left.get should be ("You did something wrong")
    }

    it ("should throw an UnauthorizedException if the baseClient returns a 401") {
      val client = new ApiJsonClient(401, JsNull)
      a [UnauthorizedException] should be thrownBy { Await(client.create("/api")) }
    }

    it ("should throw a ApiJsonClient if the baseClient returns a 500") {
      val client = new ApiJsonClient(500, JsNull)
      a [ServerErrorException] should be thrownBy { Await(client.create("/api")) }
    }

    it ("should throw an UnexpectedStatusException if the baseClient returns something besides a 201, 401, 404, 409, 422 or 500") {
      val client = new ApiJsonClient(302, JsNull)
      a [UnexpectedStatusException] should be thrownBy { Await(client.create("/api")) }
    }

    it ("should return a JSON value") {
      val client = new ApiJsonClient(201, Json.obj("abc" -> 123))
      val response = Await(client.create("/api"))
      (response.right.get \ "abc").as[Int] should be (123)
    }

  }

}
