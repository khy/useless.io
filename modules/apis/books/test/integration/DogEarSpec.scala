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

import models.books.{Edition, DogEar, Provider}
import test.util._

class DogEarSpec extends IntegrationSpec {

  "POST /dogEars" must {

    "return an error if the request isn't authenticated" in {
      val response = await {
        WS.url(s"http://localhost:$port/dogEars").
          post(Json.obj(
            "isbn" -> "1112333444445",
            "pageNumber" -> 50,
            "note" -> "Some thoughts..."
          ))
      }

      response.status mustBe UNAUTHORIZED
    }

    "respond with an error if the page number is less than 1" in {
      val response1 = await { request("/dogEars").post(Json.obj(
        "isbn" -> "1112333444445",
        "pageNumber" -> 0,
        "note" -> "Where am I?"
      )) }
      response1.status mustBe CONFLICT

      val error1 = Json.parse(response1.body).as[Seq[JsValue]].head
      (error1 \ "key").as[String] mustBe "pageNumber"
      val message1 = (error1 \ "messages").as[Seq[JsValue]].head
      (message1 \ "key").as[String] mustBe "invalid-page-number"
      (message1 \ "details" \ "specified-page-number").as[String] mustBe "0"
      (message1 \ "details" \ "minimum-page-number").as[String] mustBe "1"

      val response2 = await { request("/dogEars").post(Json.obj(
        "isbn" -> "1112333444445",
        "pageNumber" -> -1,
        "note" -> "Where am I?"
      )) }
      response2.status mustBe CONFLICT

      val error2 = Json.parse(response2.body).as[Seq[JsValue]].head
      (error2 \ "key").as[String] mustBe "pageNumber"
      val message2 = (error2 \ "messages").as[Seq[JsValue]].head
      (message2 \ "key").as[String] mustBe "invalid-page-number"
      (message2 \ "details" \ "specified-page-number").as[String] mustBe "-1"
      (message2 \ "details" \ "minimum-page-number").as[String] mustBe "1"
    }

    "respond with an error if the page number is greater than the edition page count" in {
      val response1 = await { request("/dogEars").post(Json.obj(
        "isbn" -> MockEdition.theMarriagePlot1.isbn,
        "pageNumber" -> 406,
        "note" -> "At the end!"
      )) }
      response1.status mustBe CREATED

      val response2 = await { request("/dogEars").post(Json.obj(
        "isbn" -> MockEdition.theMarriagePlot1.isbn,
        "pageNumber" -> 407,
        "note" -> "Beyond the end!"
      )) }
      response2.status mustBe CONFLICT

      val error2 = Json.parse(response2.body).as[Seq[JsValue]].head
      (error2 \ "key").as[String] mustBe "pageNumber"
      val message2 = (error2 \ "messages").as[Seq[JsValue]].head
      (message2 \ "key").as[String] mustBe "invalid-page-number"
      (message2 \ "details" \ "specified-page-number").as[String] mustBe "407"
      (message2 \ "details" \ "maximum-page-number").as[String] mustBe "406"
    }

    "create a new dog ear for the specified edition of the book, and authenticated user" in {
      val postResponse = await { request("/dogEars").post(Json.obj(
        "isbn" -> MockEdition.theMarriagePlot1.isbn,
        "pageNumber" -> 50,
        "note" -> "I'm bored!"
      )) }
      postResponse.status mustBe CREATED

      val note = Json.parse(postResponse.body).as[JsValue]
      (note \ "edition" \ "isbn").as[String] mustBe MockEdition.theMarriagePlot1.isbn
      (note \ "edition" \ "pageCount").as[Int] mustBe 406
      (note \ "edition" \ "title").as[String] mustBe "The Marriage Plot"
      (note \ "edition" \ "authors").as[Seq[String]] mustBe Seq("Jeffrey Eugenides")
      (note \ "pageNumber").as[Int] mustBe 50
      (note \ "note").as[String] mustBe "I'm bored!"
      (note \ "createdBy" \ "user" \ "handle").as[String] mustBe "khy"
      (note \ "createdAt").asOpt[String] mustBe ('defined)
    }

  }

  "GET /dogEars" must {

    def buildNotes() {
      val isbn = MockEdition.theMarriagePlot1.isbn
      appHelper.clearDogEars()
      appHelper.addNote(isbn, 34, Some("This is good, guy."))
      appHelper.addNote(isbn, 57, Some("Amiright?"))
      appHelper.addNote(isbn, 68, Some("What do you think?"))
      appHelper.addNote(isbn, 103, Some("I'm feeling a little dizzy."))
      appHelper.addNote(isbn, 140, Some("..."))
    }

    "support unauthenticated requests" in {
      buildNotes()
      val response = await {
        WS.url(s"http://localhost:$port/dogEars").withQueryString("p.limit" -> "3").get
      }

      response.status mustBe OK
    }

    "return dog ears by guid" in {
      val isbn = MockEdition.theMarriagePlot1.isbn
      val noteGuid = appHelper.addNote(isbn, 56, Some("A note."))

      val response = await {
        request("/dogEars").withQueryString("guid" -> noteGuid.toString).get
      }
      response.status mustBe OK

      val note = Json.parse(response.body).as[Seq[JsValue]].head
      (note \ "note").as[String] mustBe "A note."
      (note \ "pageNumber").as[Int] mustBe 56
    }

    "return dog ears by bookTitle" in {
      appHelper.clearDogEars()
      appHelper.addNote(MockEdition.theMarriagePlot1.isbn, 56, Some("Note #1"))
      appHelper.addNote(MockEdition.iPassLikeNight1.isbn, 89, Some("Note #2"))
      appHelper.addNote(MockEdition.theMarriagePlot2.isbn, 78, Some("Note #3"))

      val response = await {
        request("/dogEars").withQueryString("bookTitle" -> "The Marriage Plot").get
      }
      response.status mustBe OK

      val dogEars = response.json.as[Seq[DogEar]]
      dogEars.length mustBe 2
      dogEars.map(_.note) must contain theSameElementsAs Seq(Some("Note #1"), Some("Note #3"))
    }

    "return the first page of results ordered by time, if no 'page' or 'order' is specified" in {
      buildNotes()
      val response = await { request("/dogEars").withQueryString("p.limit" -> "3").get() }
      response.status mustBe OK

      val dogEars = Json.parse(response.body).as[Seq[JsValue]]
      dogEars.length mustBe 3
      (dogEars(0) \ "note").as[String] mustBe "..."
      (dogEars(1) \ "note").as[String] mustBe "I'm feeling a little dizzy."
      (dogEars(2) \ "note").as[String] mustBe "What do you think?"
    }

    "return a Link header with pagination information" in {
      buildNotes()
      val response1 = await { request("/dogEars").withQueryString("p.limit" -> "3").get() }
      response1.status mustBe OK

      val linkValues = response1.header("Link").map(LinkHeader.parse(_)).get
      val nextUrl = linkValues.find(_.relation == "next").get.url
      val response2 = await { request(nextUrl).get() }

      val dogEars = Json.parse(response2.body).as[Seq[JsValue]]
      dogEars.length mustBe 2
      (dogEars(0) \ "note").as[String] mustBe "Amiright?"
      (dogEars(1) \ "note").as[String] mustBe "This is good, guy."
    }

    "return a Link header with precendence-style pagination, if specified" in {
      buildNotes()
      val response1 = await {
        request("/dogEars").withQueryString("p.limit" -> "3", "p.style" -> "precedence").get()
      }
      response1.status mustBe OK

      val linkValues = response1.header("Link").map(LinkHeader.parse(_)).get
      val nextUrl = linkValues.find(_.relation == "next").get.url
      val response2 = await { request(nextUrl).get() }

      val dogEars = Json.parse(response2.body).as[Seq[JsValue]]
      dogEars.length mustBe 2
      (dogEars(0) \ "note").as[String] mustBe "Amiright?"
      (dogEars(1) \ "note").as[String] mustBe "This is good, guy."
    }

    "return dog ears belonging to the specified accountGuids" in {
      val khyBaseRequest = request("/dogEars")(khyAccessToken)
      await { khyBaseRequest.post(Json.obj(
        "isbn" -> MockEdition.iPassLikeNight1.isbn,
        "pageNumber" -> 96,
        "note" -> "I am khy"
      )) }

      val mikeBaseRequest = request("/dogEars")(mikeAccessToken)
      await { mikeBaseRequest.post(Json.obj(
        "isbn" -> MockEdition.iPassLikeNight1.isbn,
        "pageNumber" -> 45,
        "note" -> "I am Mike"
      )) }

      val dennisBaseRequest = request("/dogEars")(dennisAccessToken)
      await { dennisBaseRequest.post(Json.obj(
        "isbn" -> MockEdition.iPassLikeNight1.isbn,
        "pageNumber" -> 99,
        "note" -> "I am Dennis"
      )) }

      val response = await {
        request("/dogEars").withQueryString(
          "accountGuid" -> mikeAccessToken.resourceOwner.guid.toString,
          "accountGuid" -> dennisAccessToken.resourceOwner.guid.toString
        ).get()
      }

      response.status mustBe OK
      val dogEars = Json.parse(response.body).as[Seq[JsValue]]
      dogEars.length mustBe 2
    }

    "return dog ears ordered by pageNumber, if specified" in {
      appHelper.clearDogEars()

      await { request("/dogEars").post(Json.obj(
        "isbn" -> MockEdition.iPassLikeNight1.isbn,
        "pageNumber" -> 123,
        "note" -> "123"
      )) }

      await { request("/dogEars").post(Json.obj(
        "isbn" -> MockEdition.iPassLikeNight1.isbn,
        "pageNumber" -> 45,
        "note" -> "45"
      )) }

      await { request("/dogEars").post(Json.obj(
        "isbn" -> MockEdition.iPassLikeNight1.isbn,
        "pageNumber" -> 92,
        "note" -> "92"
      )) }

      val response = await {
        request("/dogEars").withQueryString("p.order" -> "pageNumber").get()
      }

      response.status mustBe OK
      val dogEars = Json.parse(response.body).as[Seq[JsValue]]
      val notes = dogEars.map { note => (note \ "note").as[String] }
      notes mustBe Seq("123", "92", "45")
    }

  }

}
