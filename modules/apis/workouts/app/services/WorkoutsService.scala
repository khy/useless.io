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
import dsl.workouts.validate.WorkoutValidator

class WorkoutsService(
  dbConfig: DatabaseConfig[Driver],
  movementsService: MovementsService
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
    val movementUuids = getTasks(workout).map(_.movement).flatten

    val futReferencedMovements = db.run(Movements.filter(_.guid inSet movementUuids).result).flatMap { records =>
      movementsService.db2api(records)
    }

    for {
      referencedMovements <- futReferencedMovements
      result <- {
        val errors = WorkoutValidator.validateWorkout(workout, referencedMovements)

        if (errors.length > 0) {
          Future.successful(Left(errors))
        } else {
          val projection = Workouts.map { r =>
            (r.guid, r.schemaVersionMajor, r.schemaVersionMinor, r.parentGuids,
             r.json, r.createdByAccount, r.createdByAccessToken)
          }.returning(Workouts.map(_.guid))

          val insert = projection += ((UUID.randomUUID, 1, 0, None,
            Json.toJson(workout), accessToken.resourceOwner.guid, accessToken.guid))

          db.run(insert).flatMap { guid =>
            db.run(Workouts.filter(_.guid === guid).result).map { workouts =>
              workouts.headOption.map { workout =>
                Right(workout)
              }.getOrElse {
                throw new ResourceNotFound("workout", guid)
              }
            }
          }
        }
      }
    } yield result
  }

  def getTasks(workout: core.Workout) = {
    def _getTasks(tasks: Seq[core.AbstractTask]): Seq[core.AbstractTask] = {
      if (!tasks.isEmpty) {
        tasks ++ _getTasks(tasks.flatMap(_.tasks.getOrElse(Nil)))
      } else {
        Nil
      }
    }

    _getTasks(Seq(workout.task))
  }

}
