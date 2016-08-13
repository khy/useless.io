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

  "POST /notes" must {

    "return an error if the request isn't authenticated" in {
      val response = await {
        WS.url(s"http://localhost:$port/notes").
          post(Json.obj(
            "isbn" -> "1112333444445",
            "pageNumber" -> 50,
            "content" -> "Some thoughts..."
          ))
      }

      response.status mustBe UNAUTHORIZED
    }

    "respond with an error if the page number is less than 1" in {
      val response1 = await { baseRequest().post(Json.obj(
        "isbn" -> "1112333444445",
        "pageNumber" -> 0,
        "content" -> "Where am I?"
      )) }
      response1.status mustBe CONFLICT

      val error1 = Json.parse(response1.body).as[Seq[JsValue]].head
      (error1 \ "key").as[String] mustBe "pageNumber"
      val message1 = (error1 \ "messages").as[Seq[JsValue]].head
      (message1 \ "key").as[String] mustBe "invalid-page-number"
      (message1 \ "details" \ "specified-page-number").as[String] mustBe "0"
      (message1 \ "details" \ "minimum-page-number").as[String] mustBe "1"

      val response2 = await { baseRequest().post(Json.obj(
        "isbn" -> "1112333444445",
        "pageNumber" -> -1,
        "content" -> "Where am I?"
      )) }
      response2.status mustBe CONFLICT

      val error2 = Json.parse(response2.body).as[Seq[JsValue]].head
      (error2 \ "key").as[String] mustBe "pageNumber"
      val message2 = (error2 \ "messages").as[Seq[JsValue]].head
      (message2 \ "key").as[String] mustBe "invalid-page-number"
      (message2 \ "details" \ "specified-page-number").as[String] mustBe "-1"
      (message2 \ "details" \ "minimum-page-number").as[String] mustBe "1"
    }

    "respond with an error if the page number is greater than the edition page count" ignore {
      val response1 = await { baseRequest().post(Json.obj(
        "isbn" -> "1112333444445",
        "pageNumber" -> 164,
        "content" -> "At the end!"
      )) }
      response1.status mustBe CREATED

      val response2 = await { baseRequest().post(Json.obj(
        "isbn" -> "1112333444445",
        "pageNumber" -> 165,
        "content" -> "Beyond the end!"
      )) }
      response2.status mustBe CONFLICT

      val error2 = Json.parse(response2.body).as[Seq[JsValue]].head
      (error2 \ "key").as[String] mustBe "pageNumber"
      val message2 = (error2 \ "messages").as[Seq[JsValue]].head
      (message2 \ "key").as[String] mustBe "invalid-page-number"
      (message2 \ "details" \ "specified-page-number").as[String] mustBe "165"
      (message2 \ "details" \ "maximum-page-number").as[String] mustBe "164"
    }

    "create a new note for the specified edition of the book, and authenticated user" in {
      val postResponse = await { baseRequest().post(Json.obj(
        "isbn" -> "1112333444445",
        "pageNumber" -> 50,
        "content" -> "I'm bored!"
      )) }
      postResponse.status mustBe CREATED

      val note = Json.parse(postResponse.body).as[JsValue]
      println(note)
      // (note \ "edition" \ "guid").as[UUID] mustBe "1112333444445"
      // (note \ "edition" \ "pageCount").as[Int] mustBe 164
      // (note \ "book" \ "title").as[String] mustBe "I Pass Like Night"
      // (note \ "book" \ "author" \ "name").as[String] mustBe "Jonathan Ames"
      (note \ "pageNumber").as[Int] mustBe 50
      (note \ "content").as[String] mustBe "I'm bored!"
      (note \ "createdBy" \ "user" \ "handle").as[String] mustBe "khy"
      (note \ "createdAt").asOpt[String] mustBe ('defined)
    }

  }

  "GET /notes" must {

    def buildNotes() {
      val isbn = "9781250014764"
      Factory.addNote(isbn, 34, "This is good, guy.")
      Factory.addNote(isbn, 57, "Amiright?")
      Factory.addNote(isbn, 68, "What do you think?")
      Factory.addNote(isbn, 103, "I'm feeling a little dizzy.")
      Factory.addNote(isbn, 140, "...")
    }

    "support unauthenticated requests" ignore {
      buildNotes()
      val response = await {
        WS.url(s"http://localhost:$port/notes").withQueryString("p.limit" -> "3").get
      }

      response.status mustBe OK
    }

    "return notes by guid" ignore {
      val isbn = "9781250014764"
      val noteGuid = Factory.addNote(isbn, 56, "A note.")

      val response = await {
        baseRequest(url = Some(s"http://localhost:$port/notes")).
          withQueryString("guid" -> noteGuid.toString).get
      }
      response.status mustBe OK

      val note = Json.parse(response.body).as[Seq[JsValue]].head
      (note \ "content").as[String] mustBe "A note."
      (note \ "pageNumber").as[Int] mustBe 56
    }

    "return the first page of results ordered by time, if no 'page' or 'order' is specified" ignore {
      buildNotes()
      val response = await { baseRequest().withQueryString("p.limit" -> "3").get() }
      response.status mustBe OK

      val notes = Json.parse(response.body).as[Seq[JsValue]]
      notes.length mustBe 3
      (notes(0) \ "content").as[String] mustBe "..."
      (notes(1) \ "content").as[String] mustBe "I'm feeling a little dizzy."
      (notes(2) \ "content").as[String] mustBe "What do you think?"
    }

    "return a Link header with pagination information" ignore {
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

    "return a Link header with precendence-style pagination, if specified" ignore {
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

    "return notes belonging to the specified accountGuids" ignore {
      val editionGuid = UUID.randomUUID

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

    "return notes ordered by pageNumber, if specified" ignore {
      val editionGuid = UUID.randomUUID

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
