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

  import applicationComponents._

  import dbConfig.db
  import dbConfig.driver.api._

  def createMovement(
    name: String = s"movement-${UUID.randomUUID}",
    variables: Option[Seq[Variable]] = None
  )(implicit accessToken: AccessToken): MovementRecord = await {
    movementsService.addMovement(core.Movement(name, variables), accessToken)
  }.toSuccess.value

  def deleteMovements() {
    db.run(sqlu"delete from movements")
  }

  val minimalWorkout = core.Workout(
    parentGuid = None,
    name = None,
    reps = None,
    time = None,
    score = Some("time"),
    tasks = None,
    movement = None
  )

  def createWorkout(
    workout: core.Workout = minimalWorkout
  )(implicit accessToken: AccessToken): WorkoutRecord = await {
    workoutsService.addWorkout(workout, accessToken)
  }.toSuccess.value

  def deleteWorkouts() {
    db.run(sqlu"delete from workouts")
  }

}
