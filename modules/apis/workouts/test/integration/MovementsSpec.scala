package test.workouts.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WS
import play.api.libs.json._
import org.scalatest._
import org.scalatestplus.play._
import io.useless.accesstoken.AccessToken
import io.useless.http.LinkHeader

import test.workouts._

class MovementsSpec extends IntegrationSpec {

  "POST /movements" must {

    "reject unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/movements").post(Json.obj())
      }

      response.status mustBe UNAUTHORIZED
    }

  }

}
