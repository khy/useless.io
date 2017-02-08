package test.workouts

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import init.workouts.AbstractApplicationComponents
import db.workouts._
import models.workouts._
import JsonImplicits._

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

  def createWorkout(
    rawJson: String
  )(implicit accessToken: AccessToken): WorkoutRecord = await {
    Json.parse(rawJson).validate[core.Workout].fold(
      error => throw new RuntimeException("Invalid workout JSON: " + rawJson),
      workout => workoutsService.addWorkout(workout, accessToken)
    )
  }.toSuccess.value

  def createWorkout(
    parentGuid: Option[UUID] = None,
    time: Option[Measurement] = None,
    score: Option[String] = Some("time"),
    movement: Option[MovementRecord] = None
  )(implicit accessToken: AccessToken): WorkoutRecord = await {
    val workout = core.Workout(
      parentGuid = parentGuid,
      name = None,
      reps = None,
      time = time,
      score = score,
      tasks = None,
      movement = movement.orElse { Some(createMovement()) }.map { movement =>
        core.TaskMovement(
          guid = movement.guid,
          score = None,
          variables = None
        )
      }
    )

    workoutsService.addWorkout(workout, accessToken)
  }.toSuccess.value

  def deleteWorkouts() {
    db.run(sqlu"delete from workouts")
  }

}
