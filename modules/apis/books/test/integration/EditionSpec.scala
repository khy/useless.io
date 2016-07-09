package test.integration

import java.util.UUID
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import play.api.libs.ws.WS
import play.api.libs.json._

import test.util._

class EditionSpec extends DefaultSpec {

  def baseRequest = {
    WS.url(s"http://localhost:$port/editions").
      withHeaders(("Authorization" -> accessToken.guid.toString))
  }

  "POST /editions" must {

    "return an error if the request isn't authenticated" in {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")
      val response = await {
        WS.url(s"http://localhost:$port/editions").
          post(Json.obj(
            "bookGuid" -> bookGuid,
            "pageCount" -> 100
          ))
      }

      response.status mustBe UNAUTHORIZED
    }

    "respond with an error if the specified book doesn't exist" in {
      val guid = UUID.randomUUID
      val response = await { baseRequest.post(Json.obj(
        "bookGuid" -> guid,
        "pageCount" -> 164
      )) }

      response.status mustBe CONFLICT
      val error = Json.parse(response.body).as[JsValue]
      (error \ "key").as[String] mustBe "unknown-book"
      (error \ "details" \ "guid").as[String] mustBe guid.toString
    }

    "respond with an error if the page count is less than 1" in {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")

      val response1 = await { baseRequest.post(Json.obj(
        "bookGuid" -> bookGuid,
        "pageCount" -> 0
      )) }
      response1.status mustBe CONFLICT

      val error1 = Json.parse(response1.body).as[JsValue]
      (error1 \ "key").as[String] mustBe "invalid-page-count"
      (error1 \ "details" \ "specified-page-count").as[String] mustBe "0"
      (error1 \ "details" \ "minimum-page-count").as[String] mustBe "1"

      val response2 = await { baseRequest.post(Json.obj(
        "bookGuid" -> bookGuid,
        "pageCount" -> -1
      )) }
      response2.status mustBe CONFLICT

      val error2 = Json.parse(response2.body).as[JsValue]
      (error2 \ "key").as[String] mustBe "invalid-page-count"
      (error2 \ "details" \ "specified-page-count").as[String] mustBe "-1"
      (error2 \ "details" \ "minimum-page-count").as[String] mustBe "1"
    }

    "create new editions for a book, idempotently" in {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")

      val postResponse1 = await { baseRequest.post(Json.obj(
        "bookGuid" -> bookGuid,
        "pageCount" -> 164
      )) }
      postResponse1.status mustBe CREATED

      val postResponse2 = await { baseRequest.post(Json.obj(
        "bookGuid" -> bookGuid,
        "pageCount" -> 164
      )) }
      postResponse2.status mustBe CREATED

      val postResponse3 = await { baseRequest.post(Json.obj(
        "bookGuid" -> bookGuid,
        "pageCount" -> 174
      )) }
      postResponse3.status mustBe CREATED

      val bookResponse = await {
        WS.url(s"http://localhost:$port/books").
          withHeaders(("Authorization" -> accessToken.guid.toString)).
          withQueryString("title" -> "I Pass Like Night").get
      }
      val books = Json.parse(bookResponse.body).as[Seq[JsValue]]
      val iPassLikeNight = books.find { json =>
        (json \ "title").as[String] == "I Pass Like Night"
      }.get

      val editions = (iPassLikeNight \ "editions").as[Seq[JsValue]]
      editions.length mustBe (2)
      editions.find { json => (json \ "pageCount").as[Int] == 164 } mustBe defined
      editions.find { json => (json \ "pageCount").as[Int] == 174 } mustBe defined
    }

  }

}
