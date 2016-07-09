package test.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WS
import play.api.libs.json._
import org.scalatest._
import org.scalatestplus.play._
import io.useless.accesstoken.AccessToken
import io.useless.http.LinkHeader

import test.util._

class NoteSpec extends DefaultSpec {

  def baseRequest(
    url: Option[String] = None,
    _accessToken: Option[AccessToken] = None
  ) = {
    WS.url(url.getOrElse(s"http://localhost:$port/notes")).
      withHeaders(("Authorization" -> _accessToken.getOrElse(accessToken).guid.toString))
  }

  "GET /notes/:guid" must {

    "support authenticated requests" in {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")
      val editionGuid = Factory.addEdition(bookGuid, pageCount = 164)
      val noteGuid = Factory.addNote(editionGuid, 34, "A note.")

      val response = await {
        WS.url(s"http://localhost:$port/notes/$noteGuid").get
      }
      response.status mustBe OK
    }

    "return a 404 if a non-existant GUID is specified" in {
      val nonExistantGuid = UUID.randomUUID
      val response = await {
        baseRequest(url = Some(s"http://localhost:$port/notes/$nonExistantGuid")).get
      }
      response.status mustBe NOT_FOUND
    }

    "return the note corresponding to the specified GUID, if one exists" in {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")
      val editionGuid = Factory.addEdition(bookGuid, pageCount = 164)
      val noteGuid = Factory.addNote(editionGuid, 56, "A note.")

      val response = await {
        baseRequest(url = Some(s"http://localhost:$port/notes/$noteGuid")).get
      }
      response.status mustBe OK

      val note = Json.parse(response.body).as[JsValue]
      (note \ "content").as[String] mustBe "A note."
      (note \ "pageNumber").as[Int] mustBe 56
    }

  }

  "POST /notes" must {

    "return an error if the request isn't authenticated" in {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")
      val editionGuid = Factory.addEdition(bookGuid = bookGuid, pageCount = 164)
      val response = await {
        WS.url(s"http://localhost:$port/notes").
          post(Json.obj(
            "editionGuid" -> editionGuid,
            "pageNumber" -> 50,
            "content" -> "Some thoughts..."
          ))
      }

      response.status mustBe UNAUTHORIZED
    }

    "respond with an error if the page number is less than 1" in {
      val bookGuid = Factory.addBook(authorName = "Jonathan Ames", title = "I Pass Like Night")
      val editionGuid = Factory.addEdition(bookGuid = bookGuid, pageCount = 164)

      val response1 = await { baseRequest().post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 0,
        "content" -> "Where am I?"
      )) }
      response1.status mustBe CONFLICT

      val error1 = Json.parse(response1.body).as[JsValue]
      (error1 \ "key").as[String] mustBe "invalid-page-number"
      (error1 \ "details" \ "specified-page-number").as[String] mustBe "0"
      (error1 \ "details" \ "minimum-page-number").as[String] mustBe "1"

      val response2 = await { baseRequest().post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> -1,
        "content" -> "Where am I?"
      )) }
      response2.status mustBe CONFLICT

      val error2 = Json.parse(response2.body).as[JsValue]
      (error2 \ "key").as[String] mustBe "invalid-page-number"
      (error2 \ "details" \ "specified-page-number").as[String] mustBe "-1"
      (error2 \ "details" \ "minimum-page-number").as[String] mustBe "1"
    }

    "respond with an error if the page number is greater than the edition page count" in {
      val bookGuid = Factory.addBook(authorName = "Jonathan Ames", title = "I Pass Like Night")
      val editionGuid = Factory.addEdition(bookGuid = bookGuid, pageCount = 164)

      val response1 = await { baseRequest().post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 164,
        "content" -> "At the end!"
      )) }
      response1.status mustBe CREATED

      val response2 = await { baseRequest().post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 165,
        "content" -> "Beyond the end!"
      )) }
      response2.status mustBe CONFLICT

      val error2 = Json.parse(response2.body).as[JsValue]
      (error2 \ "key").as[String] mustBe "invalid-page-number"
      (error2 \ "details" \ "specified-page-number").as[String] mustBe "165"
      (error2 \ "details" \ "maximum-page-number").as[String] mustBe "164"
    }

    "create a new note for the specified edition of the book, and authenticated user" in {
      val authorGuid = Factory.addAuthor("Jonathan Ames")
      val bookGuid = Factory.addBook(authorGuid = authorGuid, title = "I Pass Like Night")
      val editionGuid = Factory.addEdition(bookGuid = bookGuid, pageCount = 164)

      val postResponse = await { baseRequest().post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 50,
        "content" -> "I'm bored!"
      )) }
      postResponse.status mustBe CREATED

      val note = Json.parse(postResponse.body).as[JsValue]
      (note \ "edition" \ "guid").as[UUID] mustBe editionGuid
      (note \ "edition" \ "pageCount").as[Int] mustBe 164
      (note \ "book" \ "guid").as[UUID] mustBe bookGuid
      (note \ "book" \ "title").as[String] mustBe "I Pass Like Night"
      (note \ "book" \ "author" \ "guid").as[UUID] mustBe authorGuid
      (note \ "book" \ "author" \ "name").as[String] mustBe "Jonathan Ames"
      (note \ "pageNumber").as[Int] mustBe 50
      (note \ "content").as[String] mustBe "I'm bored!"
      (note \ "createdBy" \ "user" \ "handle").as[String] mustBe "khy"
      (note \ "createdAt").asOpt[String] mustBe ('defined)
    }

  }

  "GET /notes" must {

    def buildNotes() {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")
      val editionGuid = Factory.addEdition(bookGuid, pageCount = 164)
      Factory.addNote(editionGuid, 34, "This is good, guy.")
      Factory.addNote(editionGuid, 57, "Amiright?")
      Factory.addNote(editionGuid, 68, "What do you think?")
      Factory.addNote(editionGuid, 103, "I'm feeling a little dizzy.")
      Factory.addNote(editionGuid, 140, "...")
    }

    "support unauthenticated requests" in {
      buildNotes()
      val response = await {
        WS.url(s"http://localhost:$port/notes").withQueryString("p.limit" -> "3").get
      }

      response.status mustBe OK
    }

    "return the first page of results ordered by time, if no 'page' or 'order' is specified" in {
      buildNotes()
      val response = await { baseRequest().withQueryString("p.limit" -> "3").get() }
      response.status mustBe OK

      val notes = Json.parse(response.body).as[Seq[JsValue]]
      notes.length mustBe 3
      (notes(0) \ "content").as[String] mustBe "..."
      (notes(1) \ "content").as[String] mustBe "I'm feeling a little dizzy."
      (notes(2) \ "content").as[String] mustBe "What do you think?"
    }

    "return a Link header with pagination information" in {
      buildNotes()
      val response1 = await { baseRequest().withQueryString("p.limit" -> "3").get() }
      response1.status mustBe OK

      val linkValues = response1.header("Link").map(LinkHeader.parse(_)).get
      val nextUrl = linkValues.find(_.relation == "next").get.url
      val response2 = await { baseRequest(Some(nextUrl)).get() }

      val notes = Json.parse(response2.body).as[Seq[JsValue]]
      notes.length mustBe 2
      (notes(0) \ "content").as[String] mustBe "Amiright?"
      (notes(1) \ "content").as[String] mustBe "This is good, guy."
    }

    "return a Link header with precendence-style pagination, if specified" in {
      buildNotes()
      val response1 = await {
        baseRequest().withQueryString("p.limit" -> "3", "p.style" -> "precedence").get()
      }
      response1.status mustBe OK

      val linkValues = response1.header("Link").map(LinkHeader.parse(_)).get
      val nextUrl = linkValues.find(_.relation == "next").get.url
      val response2 = await { baseRequest(Some(nextUrl)).get() }

      val notes = Json.parse(response2.body).as[Seq[JsValue]]
      notes.length mustBe 2
      (notes(0) \ "content").as[String] mustBe "Amiright?"
      (notes(1) \ "content").as[String] mustBe "This is good, guy."
    }

    "return notes belonging to the specified accountGuids" in {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")
      val editionGuid = Factory.addEdition(bookGuid = bookGuid, pageCount = 164)

      val khyBaseRequest = baseRequest(_accessToken = Some(khyAccessToken))
      await { khyBaseRequest.post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 96,
        "content" -> "I am khy"
      )) }

      val mikeBaseRequest = baseRequest(_accessToken = Some(mikeAccessToken))
      await { mikeBaseRequest.post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 45,
        "content" -> "I am Mike"
      )) }

      val dennisBaseRequest = baseRequest(_accessToken = Some(dennisAccessToken))
      await { dennisBaseRequest.post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 99,
        "content" -> "I am Dennis"
      )) }

      val response = await {
        baseRequest().withQueryString(
          "accountGuid" -> mikeAccessToken.resourceOwner.guid.toString,
          "accountGuid" -> dennisAccessToken.resourceOwner.guid.toString
        ).get()
      }

      response.status mustBe OK
      val notes = Json.parse(response.body).as[Seq[JsValue]]
      notes.length mustBe 2
    }

    "return notes ordered by pageNumber, if specified" in {
      val bookGuid = Factory.addBook(title = "I Pass Like Night", authorName = "Jonathan Ames")
      val editionGuid = Factory.addEdition(bookGuid = bookGuid, pageCount = 164)

      await { baseRequest().post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 123,
        "content" -> "123"
      )) }

      await { baseRequest().post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 45,
        "content" -> "45"
      )) }

      await { baseRequest().post(Json.obj(
        "editionGuid" -> editionGuid,
        "pageNumber" -> 92,
        "content" -> "92"
      )) }

      val response = await {
        baseRequest().withQueryString("p.order" -> "pageNumber").get()
      }

      response.status mustBe OK
      val notes = Json.parse(response.body).as[Seq[JsValue]]
      val contents = notes.map { note => (note \ "content").as[String] }
      contents mustBe Seq("123", "92", "45")
    }

  }

}
