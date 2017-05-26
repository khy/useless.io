package test.workouts.integration

import java.util.UUID
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._

import models.workouts._
import test.workouts._

class WorkoutSpec extends IntegrationSpec {

  "POST /workouts" must {

    "reject unauthenticated requests" in {
      val response = await {
        unauthenticatedRequest("/workouts").post(Json.obj())
      }

      response.status mustBe UNAUTHORIZED
    }

  }

}
