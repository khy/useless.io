package test.integration

import java.util.UUID
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WS
import play.api.libs.json._

import test.util._

class BookSpec extends DefaultSpec {

  def baseRequest(path: Option[String] = None) = {
    WS.url(s"http://localhost:$port" + path.getOrElse("/books")).
      withHeaders(("Authorization" -> accessToken.guid.toString))
  }

  "GET /books/:guid" must {

    "return a 404 if no book corresponds to the specified GUID" in {
      val guid = UUID.randomUUID
      val response = await { baseRequest(Some(s"/books/$guid")).get }
      response.status mustBe NOT_FOUND
    }

    "return the book corresponding to the specified GUID" in {
      val guid = Factory.addBook("The Corrections", "Jonathan Franzen")
      val response = await { baseRequest(Some(s"/books/$guid")).get }

      response.status mustBe OK
      val book = Json.parse(response.body).as[JsValue]
      (book \ "title").as[String] mustBe "The Corrections"
    }

  }

  "GET /books" must {

    "return an error if the request isn't authenticated" in {
      val response = await {
        WS.url(s"http://localhost:$port/books").
          withQueryString("title" -> "The Corrections").get
      }

      response.status mustBe UNAUTHORIZED
    }

    "return exact matches on name" in {
      Factory.addBook("The Corrections", "Jonathan Franzen")
      val request = baseRequest().withQueryString("title" -> "The Corrections")
      val response = await { request.get }

      response.status mustBe OK
      val books = Json.parse(response.body).as[Seq[JsValue]]
      val theCorrections = books.find { json =>
        (json \ "title").as[String] == "The Corrections"
      }
      theCorrections mustBe defined
      (theCorrections.get \ "author" \ "name").as[String] mustBe "Jonathan Franzen"
    }

    "return matches on significant words" in {
      Factory.addBook("The Corrections", "Jonathan Franzen")
      val request = baseRequest().withQueryString("title" -> "corr")
      val response = await { request.get }

      response.status mustBe OK
      val books = Json.parse(response.body).as[Seq[JsValue]]
      val theCorrections = books.find { json =>
        (json \ "title").as[String] == "The Corrections"
      }
      theCorrections mustBe defined
    }

    "return matches on the last words in the title" in {
      Factory.addBook("The Virgin Suicides", "Jeffery Eugenides")
      val request = baseRequest().withQueryString("title" -> "sui")
      val response = await { request.get }

      response.status mustBe OK
      val books = Json.parse(response.body).as[Seq[JsValue]]
      val theVirginSuicides = books.find { json =>
        (json \ "title").as[String] == "The Virgin Suicides"
      }
      theVirginSuicides mustBe defined
    }

    "return matches with the appropriate author joined" in {
      Factory.addAuthor("Jonathan Franzen")
      Factory.addBook("The Virgin Suicides", "Jeffery Eugenides")

      val request = baseRequest().withQueryString("title" -> "virgin")
      val response = await { request.get }

      response.status mustBe OK
      val books = Json.parse(response.body).as[Seq[JsValue]]
      books.length mustBe 1
    }

  }

  "POST /books" must {

    "return an error if the request isn't authenticated" in {
      val authorGuid = Factory.addAuthor("Jonathan Ames")
      val response = await {
        WS.url(s"http://localhost:$port/authors").
          post(Json.obj(
            "title" -> "I Pass Like Night",
            "authorGuid" -> authorGuid
          ))
      }

      response.status mustBe UNAUTHORIZED
    }

    "create a new book" in {
      val authorGuid = Factory.addAuthor("Jonathan Ames")
      val postResponse = await { baseRequest().post(Json.obj(
        "title" -> "I Pass Like Night",
        "authorGuid" -> authorGuid
      )) }

      postResponse.status mustBe CREATED
      val getResponse = await {
        baseRequest().withQueryString("title" -> "I Pass Like Night").get
      }
      val books = Json.parse(getResponse.body).as[Seq[JsValue]]
      val iPassLikeNight = books.find { json =>
        (json \ "title").as[String] == "I Pass Like Night"
      }
      iPassLikeNight mustBe defined
    }

    "be idempotent" in {
      val authorGuid = Factory.addAuthor("Jeffery Eugenides")
      Factory.addBook("The Virgin Suicides", authorGuid)

      val postResponse = await { baseRequest().post(Json.obj(
        "title" -> "The Virgin Suicides",
        "authorGuid" -> authorGuid
      )) }

      postResponse.status mustBe CREATED
      val getResponse = await {
        baseRequest().withQueryString("title" -> "The Virgin Suicides").get
      }
      val authors = Json.parse(getResponse.body).as[Seq[JsValue]]
      val theVirginSuicides = authors.find { json =>
        (json \ "title").as[String] == "The Virgin Suicides"
      }
      theVirginSuicides mustBe defined
    }

  }

}
