package services.workouts

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.{Json, JsPath}
import play.api.data.validation.ValidationError
import slick.backend.DatabaseConfig
import io.useless.Message
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._
import io.useless.exception.service._

import db.workouts._
import models.workouts._

class WorkoutsService(
  dbConfig: DatabaseConfig[Driver]
) {

  import dbConfig.db
  import dbConfig.driver.api._
  import dbConfig.driver.api.playJsonColumnExtensionMethods

  def db2api(records: Seq[WorkoutRecord]): Future[Seq[Workout]] = {
    Future.successful(
      records.map { record =>
        Json.fromJson[core.Workout](record.json).fold(
          error => throw new InvalidState(s"Invalid movement JSON from DB [$error]"),
          workout => Workout(
            guid = record.guid,
            name = workout.name,
            score = workout.score,
            variables = workout.variables,
            task = workout.task,
            createdAt = record.createdAt,
            createdByAccount = record.createdByAccount,
            deletedAt = record.deletedAt,
            deletedByAccount = record.deletedByAccount
          )
        )
      }
    )
  }

  def addWorkout(
    workout: core.Workout,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Either[Seq[(JsPath, Seq[ValidationError])], WorkoutRecord]] = {
    Future.successful(Left(Nil))
  }

}
