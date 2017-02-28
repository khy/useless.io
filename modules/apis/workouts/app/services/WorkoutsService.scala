package services.workouts

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
import dsl.workouts.WorkoutDsl
import models.workouts._
import models.workouts.JsonImplicits._

class WorkoutsService(
  dbConfig: DatabaseConfig[Driver],
  movementsService: MovementsService,
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
            parentGuid = workout.parentGuid,
            name = workout.name,
            reps = workout.reps,
            time = workout.time,
            score = workout.score,
            tasks = workout.tasks,
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
          workout.json +>> "parentGuid" inSet parentGuids.map(_.toString)
        }
      }

      isChild.foreach { isChild =>
        if (isChild) {
          query = query.filter { _.json.+>("parentGuid").?.isDefined }
        } else {
          query = query.filter { _.json.+>("parentGuid").?.isEmpty }
        }
      }

      db.run(query.result).map { case workoutRecords =>
        PaginatedResult.build(workoutRecords, paginationParams)
      }
    }
  }

  private def findWorkout(guid: UUID)(implicit ec: ExecutionContext): Future[Option[Workout]] = {
    val query = Workouts.filter(_.guid === guid)
    db.run(query.result).flatMap(db2api).map(_.headOption)
  }

  def addWorkout(
    workout: core.Workout,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[WorkoutRecord]] = {
    val subTasks = WorkoutDsl.getSubTasks(workout)
    val taskMovements = workout.movement.toSeq ++ subTasks.flatMap(_.movement)

    // Fetch the actual movements referenced by the task movements
    val futReferencedMovements = movementsService.
      getMovementsByGuid(taskMovements.map(_.guid)).
      flatMap(movementsService.db2api)

    // Find the "ancestry" of the workout (limited to 2 for now)
    val futAncestry = for {
      optParent <- workout.parentGuid.map { parentGuid =>
        findWorkout(parentGuid)
      }.getOrElse(Future.successful(None))
      optGrandParent <- optParent.flatMap(_.parentGuid).map { grandParentGuid =>
        findWorkout(grandParentGuid)
      }.getOrElse(Future.successful(None))
    } yield Seq(optParent, optGrandParent).flatten

    for {
      referencedMovements <- futReferencedMovements
      ancestry <- futAncestry
      result <- {
        val effectiveWorkout = WorkoutDsl.mergeWorkouts(workout, ancestry)
        val validationErrors = WorkoutDsl.validateWorkout(effectiveWorkout, referencedMovements)

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
    } yield result
  }

}
