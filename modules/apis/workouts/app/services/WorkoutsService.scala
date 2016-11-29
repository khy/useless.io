package services.workouts

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import slick.backend.DatabaseConfig
import io.useless.Message
import io.useless.accesstoken.AccessToken
import io.useless.validation._
import io.useless.exception.service._

import db.workouts._
import models.workouts._
import models.workouts.JsonImplicits._

class WorkoutsService(
  dbConfig: DatabaseConfig[Driver]
) {

  import dbConfig.db
  import dbConfig.driver.api._

  def db2api(records: Seq[WorkoutRecord]): Future[Seq[Workout]] = {
    Future.successful(
      records.map { record =>
        Json.fromJson[core.Workout](record.json).fold(
          error => throw new InvalidState(s"Invalid workout JSON from DB [$error]"),
          workout => Workout(
            guid = record.guid,
            parentGuid = workout.parentGuid,
            name = workout.name,
            reps = workout.reps,
            score = workout.score,
            tasks = workout.tasks,
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
  )(implicit ec: ExecutionContext): Future[Validation[WorkoutRecord]] = {
    // def getMovementGuids(subTasks: Seq[core.SubTask]): Seq[UUID] = {
    //   subTasks.flatMap(_.movement.map(_.guid)) ++ getMovementGuids(subTasks.flatMap(_.tasks.getOrElse(Nil)))
    // }

    val movementGuids = workout.movement.map(_.guid).toSeq ++ workout.tasks.getOrElse(Nil).flatMap(_.movement.map(_.guid))
    //getMovementGuids(workout.tasks.getOrElse(Nil))

    val movementsQuery = Movements.filter(_.guid.inSet(movementGuids))
    val futOptUnknownGuidErrors = db.run(movementsQuery.result).map { movements =>
      val badGuids = movementGuids.filterNot { movementGuid =>
        movements.map(_.guid).contains(movementGuid)
      }

      if (badGuids.size > 0) {
        Some(Errors.scalar(badGuids.map { badGuid =>
          Message(
            key = "unknownWorkoutGuid",
            details = "guid" -> badGuid.toString
          )
        }))
      } else {
        None
      }
    }

    for {
      optUnknownGuidErrors <- futOptUnknownGuidErrors
      result <- {
        optUnknownGuidErrors.map { unknownGuidErrors =>
          Future.successful(Validation.failure(Seq(unknownGuidErrors)))
        }.getOrElse {
          val projection = Workouts.map { r =>
            (r.guid, r.schemaVersionMajor, r.schemaVersionMinor, r.json,
             r.createdByAccount, r.createdByAccessToken)
          }.returning(Workouts.map(_.guid))

          val insert = projection += ((UUID.randomUUID, 1, 0, Json.toJson(workout),
            accessToken.resourceOwner.guid, accessToken.guid))

          db.run(insert).flatMap { guid =>
            val query = Workouts.filter(_.guid === guid)
            db.run(query.result).map { workouts =>
              workouts.headOption.map { workout =>
                Validation.success(workout)
              }.getOrElse {
                throw new ResourceNotFound("workout", guid)
              }
            }
          }
        }
      }
    } yield result
  }

}
