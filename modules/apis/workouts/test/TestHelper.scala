package test.workouts

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import init.workouts.AbstractApplicationComponents
import db.workouts._
import models.workouts._

class TestHelper(
  applicationComponents: AbstractApplicationComponents
) {

  def createMovement(
    name: String = s"movement-${UUID.randomUUID}",
    variables: Option[Seq[Variable]] = None
  )(implicit accessToken: AccessToken): MovementRecord = await {
    applicationComponents.movementsService.addMovement(core.Movement(name, variables), accessToken)
  }.toSuccess.value

}
