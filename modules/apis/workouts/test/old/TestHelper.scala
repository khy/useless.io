package test.workouts.old

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import init.workouts.AbstractApplicationComponents
import db.workouts._
import models.workouts.old._
import JsonImplicits._

class TestHelper(
  applicationComponents: AbstractApplicationComponents,
  newTestHelper: test.workouts.TestHelper
) {

  import applicationComponents._

  import dbConfig.db
  import dbConfig.driver.api._

  def clearDb() {
    deleteMovements()
    deleteWorkouts()
  }

  def deleteMovements() {
    db.run(sqlu"delete from movements")
  }

  def buildWorkoutFromJson(
    rawJson: String
  ) = Json.parse(rawJson).validate[core.Workout].fold(
    error => throw new RuntimeException(s"Invalid workout JSON [$error]: $rawJson"),
    workout => workout
  )

  def createWorkoutFromJson(
    rawJson: String,
    parentGuid: Option[UUID] = None
  )(implicit accessToken: AccessToken): WorkoutRecord = await {
    oldWorkoutsService.addWorkout(parentGuid, buildWorkoutFromJson(rawJson), accessToken)
  }.toSuccess.value

  def buildWorkout(
    time: Option[Measurement] = None,
    score: Option[String] = Some("time"),
    movement: Option[MovementRecord] = None
  ) = core.Workout(
    name = None,
    reps = None,
    time = time,
    score = score,
    tasks = None,
    movement = movement.map { movement =>
      core.TaskMovement(
        guid = movement.guid,
        score = None,
        variables = None
      )
    }
  )

  def createWorkout(
    parentGuid: Option[UUID] = None,
    time: Option[Measurement] = None,
    score: Option[String] = Some("time"),
    movement: Option[MovementRecord] = None
  )(implicit accessToken: AccessToken): WorkoutRecord = await {
    val workout = buildWorkout(
      time = time,
      score = score,
      movement = movement.orElse { Some(newTestHelper.createMovement()) }
    )

    oldWorkoutsService.addWorkout(parentGuid, workout, accessToken)
  }.toSuccess.value

  def deleteWorkouts() {
    db.run(sqlu"delete from workouts")
  }

}
