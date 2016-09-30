package test.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WS
import play.api.libs.json._
import org.scalatest._
import org.scalatestplus.play._

import models.books.UserEdition
import test.util._

class UserEditionsSpec extends IntegrationSpec {

  "GET /userEditions" must {

    "support unauthenticated requests" in {
      val response = await {
        WS.url(s"http://localhost:$port/userEditions").get
      }

      response.status mustBe OK
    }

    "return UserEditions for the specified User guid" in {
      appHelper.clearDogEars()
      appHelper.addDogEar(MockEdition.theMarriagePlot1.isbn, 34, Some("This is good, guy."))
      appHelper.addDogEar(MockEdition.theMarriagePlot2.isbn, 55, Some("Amiright?"))
      appHelper.addDogEar(MockEdition.iPassLikeNight1.isbn, 14, Some("I'm feeling a little dizzy."))(mikeAccessToken)

      val response = await { request(s"/userEditions?userGuid=${khyAccessToken.resourceOwner.guid}").get }
      response.status mustBe OK
      val books = response.json.as[Seq[UserEdition]]
      books.length mustBe 2
    }

  }

}
