package services.workouts.old

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import slick.backend.DatabaseConfig
import io.useless.Message
import io.useless.account.User
import io.useless.accesstoken.AccessToken
import io.useless.client.account.AccountClient
import io.useless.validation._
import io.useless.pagination._
import io.useless.exception.service._

import db.workouts._
import dsl.workouts.old.WorkoutDsl
import models.workouts.old._
import models.workouts.old.JsonImplicits._

class WorkoutsService(
  dbConfig: DatabaseConfig[Driver],
  accountClient: AccountClient
) {

  import dbConfig.db
  import dbConfig.driver.api._

  def db2api(records: Seq[WorkoutRecord])(implicit ec: ExecutionContext): Future[Seq[Workout]] = {
    val userGuids = records.flatMap { record =>
      Seq(record.createdByAccount) ++ record.deletedByAccount.toSeq
    }

    val futUsers = if (userGuids.isEmpty) {
      Future.successful(Nil)
    } else {
      accountClient.findAccounts(guids = userGuids).map { accounts =>
        accounts.flatMap {
          case user: User => Some(user)
          case _ => None
        }
      }
    }

    futUsers.map { users =>
      records.map { record =>
        Json.fromJson[core.Workout](record.json).fold(
          error => throw new InvalidState(s"Invalid workout JSON from DB [$error]"),
          workout => Workout(
            guid = record.guid,
            parentGuids = record.parentGuids,
            name = workout.name,
            reps = workout.reps,
            time = workout.time,
            score = workout.score,
            tasks = workout.tasks,
            movement = workout.movement,
            createdAt = record.createdAt,
            createdBy = users.find(_.guid == record.createdByAccount).getOrElse(User.Anon),
            deletedAt = record.deletedAt,
            deletedBy = users.find(_.guid == record.deletedByAccount)
          )
        )
      }
    }
  }

  def findWorkouts(
    guids: Option[Seq[UUID]] = None,
    parentGuids: Option[Seq[UUID]] = None,
    isChild: Option[Boolean] = None,
    rawPaginationParams: RawPaginationParams
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[WorkoutRecord]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var query: Query[WorkoutsTable, WorkoutRecord, Seq] = Workouts

      guids.foreach { guids =>
        query = query.filter(_.guid inSet guids)
      }

      parentGuids.foreach { parentGuids =>
        query = query.filter { workout =>
          workout.parentGuids @& parentGuids.toList.bind
        }
      }

      isChild.foreach { isChild =>
        if (isChild) {
          query = query.filter { _.parentGuids.isDefined }
        } else {
          query = query.filter { _.parentGuids.isEmpty }
        }
      }

      db.run(query.result).map { case workoutRecords =>
        PaginatedResult.build(workoutRecords, paginationParams)
      }
    }
  }

  private def findRawWorkouts(guids: Seq[UUID])(implicit ec: ExecutionContext): Future[Seq[Workout]] = {
    val query = Workouts.filter(_.guid.inSet(guids))
    db.run(query.result).flatMap(db2api)
  }

  def addWorkout(
    parentGuid: Option[UUID],
    workout: core.Workout,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[WorkoutRecord]] = {
    // Find the "ancestry" of the workout
    val futValAncestry = parentGuid.map { parentGuid =>
      findRawWorkouts(Seq(parentGuid)).flatMap { workouts =>
        workouts.headOption.map { workout =>
          workout.parentGuids.map { parentGuids =>
            findRawWorkouts(parentGuids)
          }.getOrElse {
            Future.successful(Nil)
          }.map { parentWorkouts =>
            Validation.success(workout +: parentWorkouts)
          }
        }.getOrElse {
          Future.successful(Validation.failure(
            key = "parentGuid",
            messageKey = "unknownWorkoutGuid",
            messageDetails = "specified" -> parentGuid.toString
          ))
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(Nil))
    }

    val subTasks = WorkoutDsl.getSubTasks(workout)
    val taskMovements = workout.movement.toSeq ++ subTasks.flatMap(_.movement)

    // Fetch the actual movements referenced by the task movements
    val futValReferencedMovements = Future.successful(Validation.success(Nil))

    for {
      valAncestry <- futValAncestry
      valReferencedMovements <- futValReferencedMovements
      result <- {
        ValidationUtil.flatMapFuture(valAncestry ++ valReferencedMovements) { case (ancestry, referencedMovements) =>
          val validationErrors = WorkoutDsl.validateWorkout(workout, referencedMovements)

          if (validationErrors.length > 0) {
            Future.successful(Validation.failure(validationErrors))
          } else {
            val ancestryErrors = ancestry.headOption.map { parent =>
              WorkoutDsl.validateAncestry(workout, parent)
            }.getOrElse(Nil)

            if (ancestryErrors.length > 0) {
              Future.successful(Validation.failure(ancestryErrors))
            } else {
              val projection = Workouts.map { r =>
                (r.guid, r.schemaVersionMajor, r.schemaVersionMinor, r.parentGuids,
                 r.json, r.createdByAccount, r.createdByAccessToken)
              }.returning(Workouts.map(_.guid))

              val parentGuids = if (parentGuid.isDefined) Some(ancestry.map(_.guid).toList) else None

              val insert = projection += ((UUID.randomUUID, 1, 0, parentGuids,
                Json.toJson(workout), accessToken.resourceOwner.guid, accessToken.guid))

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
    } yield result
  }

}
