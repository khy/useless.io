package test.integration

import java.util.UUID
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WS
import play.api.libs.json._

import test.util._

class AuthorSpec extends DefaultSpec {

  def baseRequest = {
    WS.url(s"http://localhost:$port/authors").
      withHeaders(("Authorization" -> accessToken.guid.toString))
  }

  "GET /authors" must {

    "return an error if the request isn't authenticated" in {
      val response = await {
        WS.url(s"http://localhost:$port/authors").
          withQueryString("name" -> "Jonathan Franzen").get
      }

      response.status mustBe UNAUTHORIZED
    }

    "return exact matches on name" in {
      Factory.addAuthor("Jonathan Franzen")
      val request = baseRequest.withQueryString("name" -> "Jonathan Franzen")
      val response = await { request.get }

      response.status mustBe OK
      val authors = Json.parse(response.body).as[Seq[JsValue]]
      val franzen = authors.find { json => (json \ "name").as[String] == "Jonathan Franzen" }
      franzen mustBe defined
    }

    "return partial matches on the first name" in {
      Factory.addAuthor("Jeffrey Eugenides")
      val request = baseRequest.withQueryString("name" -> "jeff")
      val response = await { request.get }

      response.status mustBe OK
      val authors = Json.parse(response.body).as[Seq[JsValue]]
      val eugenides = authors.find { json => (json \ "name").as[String] == "Jeffrey Eugenides" }
      eugenides mustBe defined
    }

    "return partial matches on the last name" in {
      Factory.addAuthor("Jonathan Ames")
      val request = baseRequest.withQueryString("name" -> "ame")
      val response = await { request.get }

      response.status mustBe OK
      val authors = Json.parse(response.body).as[Seq[JsValue]]
      val ames = authors.find { json => (json \ "name").as[String] == "Jonathan Ames" }
      ames mustBe defined
    }

  }

  "POST /authors" must {

    "return an error if the request isn't authenticated" in {
      val response = await {
        WS.url(s"http://localhost:$port/authors").
          post(Json.obj("name" -> "Jonathan Franzen"))
      }

      response.status mustBe UNAUTHORIZED
    }

    "create a new author" in {
      val body = Json.obj("name" -> "Jonathan Franzen")
      val postResponse = await { baseRequest.post(body) }

      postResponse.status mustBe CREATED
      val getResponse = await {
        baseRequest.withQueryString("name" -> "Jonathan Franzen").get
      }
      val authors = Json.parse(getResponse.body).as[Seq[JsValue]]
      val franzen = authors.find { json => (json \ "name").as[String] == "Jonathan Franzen" }
      franzen mustBe defined
    }

    "be idempotent" in {
      Factory.addAuthor("Jonathan Ames")
      val body = Json.obj("name" -> "Jonathan Ames")
      val postResponse = await { baseRequest.post(body) }

      postResponse.status mustBe CREATED
      val getResponse = await {
        baseRequest.withQueryString("name" -> "Jonathan Ames").get
      }
      val authors = Json.parse(getResponse.body).as[Seq[JsValue]]
      val ames = authors.find { json => (json \ "name").as[String] == "Jonathan Ames" }
      ames mustBe defined
    }

  }

}
