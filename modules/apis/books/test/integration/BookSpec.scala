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

import models.books.Book
import test.util._

class BookSpec extends IntegrationSpec {

  "GET /books" must {

    "support unauthenticated requests" in {
      val response = await {
        WS.url(s"http://localhost:$port/books").get
      }

      response.status mustBe OK
    }

    "return books" in {
      appHelper.clearEditionCache()
      appHelper.clearNotes()
      appHelper.addNote(MockEdition.theMarriagePlot1.isbn, 34, "This is good, guy.")
      appHelper.addNote(MockEdition.theMarriagePlot2.isbn, 55, "Amiright?")
      appHelper.addNote(MockEdition.iPassLikeNight1.isbn, 14, "I'm feeling a little dizzy.")

      val response = await { request("/books").get }
      response.status mustBe OK
      val books = response.json.as[Seq[Book]]
      books.length mustBe 2

      val theMarriagePlot = books.find(_.title == "The Marriage Plot").get
      theMarriagePlot.subtitle mustBe Some("A Novel")
      theMarriagePlot.authors mustBe Seq("Jeffrey Eugenides")
      theMarriagePlot.smallImageUrl mustBe Some("example.com/marriageplot/sm")
      theMarriagePlot.largeImageUrl mustBe Some("example.com/marriageplot/lg")

      val iPassLikeNight = books.find(_.title == "I Pass Like Night").get
      iPassLikeNight.subtitle mustBe None
      iPassLikeNight.authors mustBe Seq("Jonathan Ames")
      iPassLikeNight.smallImageUrl mustBe Some("example.com/passlikenight/sm")
      iPassLikeNight.largeImageUrl mustBe Some("example.com/passlikenight/lg")
    }

  }

}
