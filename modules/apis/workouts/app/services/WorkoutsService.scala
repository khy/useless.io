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
    def getMovementGuids(subTasks: Seq[core.SubTask]): Seq[UUID] = {
      if (!subTasks.isEmpty) {
        subTasks.flatMap(_.movement.map(_.guid)) ++
        getMovementGuids(subTasks.flatMap(_.tasks.getOrElse(Nil)))
      } else {
        Nil
      }
    }

    val movementGuids = workout.movement.map(_.guid).toSeq ++
      getMovementGuids(workout.tasks.getOrElse(Nil))

    val movementsQuery = Movements.filter(_.guid.inSet(movementGuids))

    db.run(movementsQuery.result).flatMap { movements =>
      var errors = Seq.empty[Errors]

      // Validate that all workout GUIDs are known
      val badGuids = movementGuids.filterNot { movementGuid =>
        movements.map(_.guid).contains(movementGuid)
      }

      if (badGuids.size > 0) {
        errors = errors :+ Errors.scalar(badGuids.map { badGuid =>
          Message(
            key = "unknownWorkoutGuid",
            details = "guid" -> badGuid.toString
          )
        })
      }

      val scores = workout.score.toSeq ++
        workout.movement.flatMap(_.score).toSeq ++
        workout.tasks.map(_.flatMap(_.movement.flatMap(_.score))).getOrElse(Nil)

      // Validate that there's exactly one score
      if (scores.size == 0) {
        errors = errors :+ Errors.scalar(Seq(Message(key = "noScoreSpecified")))
      }

      if (scores.size > 1) {
        errors = errors :+ Errors.scalar(Seq(Message(key = "multipleScoresSpecified")))
      }

      // Validate that, if there's a top-level score, that it's either 'time' or 'reps'
      workout.score.foreach { topLevelScore =>
        if (topLevelScore != "time" && topLevelScore != "reps") {
          errors = errors :+ Errors.scalar(Seq(Message(key = "invalidTopLevelScore", "score" -> topLevelScore)))
        }
      }

      if (!errors.isEmpty) {
        Future.successful(Validation.failure(errors))
      } else {
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
  }

}
