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

  def findWorkouts(
    guids: Option[Seq[UUID]] = None
  ): Future[Seq[WorkoutRecord]] = {
    var query: Query[WorkoutsTable, WorkoutRecord, Seq] = Workouts

    guids.foreach { guids =>
      query = query.filter(_.guid inSet guids)
    }

    db.run(query.result)
  }

  def addWorkout(
    workout: core.Workout,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[WorkoutRecord]] = {
    // Find all movements referenced in the workout
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
    val futMovements = db.run(movementsQuery.result)

    // Find the "ancestry" of the workout (limited to 2 for now)
    val futAncestry = for {
      optParent <- workout.parentGuid.map { parentGuid =>
        findWorkouts(guids = Some(Seq(parentGuid))).flatMap(db2api).map(_.headOption)
      }.getOrElse(Future.successful(None))
      optGrandParent <- optParent.flatMap(_.parentGuid).map { grandParentGuid =>
        findWorkouts(guids = Some(Seq(grandParentGuid))).flatMap(db2api).map(_.headOption)
      }.getOrElse(Future.successful(None))
    } yield Seq(optParent, optGrandParent).flatten

    for {
      movements <- futMovements
      ancestry <- futAncestry
      result <- {
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

        // If the workout does not have a parent,
        if (ancestry.length == 0) {
          // it must have exactly one score,
          if (scores.size == 0) {
            errors = errors :+ Errors.scalar(Seq(Message(key = "noScoreSpecified")))
          } else if (scores.size > 1) {
            errors = errors :+ Errors.scalar(Seq(Message(key = "multipleScoresSpecified")))
          }

          // and the top-level score, if it exists, must be either 'time' or 'reps'.
          workout.score.foreach { topLevelScore =>
            if (topLevelScore != "time" && topLevelScore != "reps") {
              errors = errors :+ Errors.scalar(Seq(Message(key = "invalidTopLevelScore", "score" -> topLevelScore)))
            }
          }
        } else {
          // If the workout does have a parent, it cannot have a score.
          if (scores.size > 0) {
            errors = errors :+ Errors.scalar(Seq(Message(key = "scoreSpecifiedByChild")))
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
    } yield result
  }

}
