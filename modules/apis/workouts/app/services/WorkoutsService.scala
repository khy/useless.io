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
    // Recursively find all subtasks within the workout
    def getSubTasks(subTasks: Seq[core.SubTask]): Seq[core.SubTask] = {
      if (!subTasks.isEmpty) {
        subTasks ++ getSubTasks(subTasks.flatMap(_.tasks.getOrElse(Nil)))
      } else {
        Nil
      }
    }

    val subTasks = getSubTasks(workout.tasks.getOrElse(Nil))
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
        var errors = Seq.empty[Errors]

        // A workout with a parent _cannot_ have a score. A workout without
        // a parent _must_ have a score.
        val scores = workout.score.toSeq ++ taskMovements.flatMap(_.score)

        if (ancestry.length == 0) {
          if (scores.size == 0) {
            errors = errors :+ Errors.scalar(Seq(Message(key = "noScoreSpecified")))
          } else if (scores.size > 1) {
            errors = errors :+ Errors.scalar(Seq(Message(key = "multipleScoresSpecified")))
          }
        } else {
          if (scores.size > 0) {
            errors = errors :+ Errors.scalar(Seq(Message(key = "scoreSpecifiedByChild")))
          }
        }

        // A workout and all subtasks must have either a movement or at least one task
        if (workout.movement.isEmpty && workout.tasks.map(_.isEmpty).getOrElse(true)) {
          errors = errors :+ Errors.scalar(Seq(Message(key = "noTaskMovementOrSubTask")))
        }

        val emptySubTasks = subTasks.filter { subTask =>
          subTask.movement.isEmpty && subTask.tasks.map(_.isEmpty).getOrElse(true)
        }

        if (!emptySubTasks.isEmpty) {
          errors = errors :+ Errors.scalar(Seq(Message(key = "noTaskMovementOrSubTask")))
        }

        def taskMovementErrors(taskMovement: core.TaskMovement): Option[Errors] = {
          referencedMovements.find(_.guid == taskMovement.guid).map { referencedMovement =>
            // If a task movement has a score, it must reference a free variable
            // either in the task movement itself, or in the referenced movement.
            taskMovement.score.flatMap { score =>
              val freeVariableNames = (taskMovement.variables ++ referencedMovement.variables).flatten.filter { variable =>
                variable.dimension.isDefined
              }.map(_.name).toSeq

              if (!freeVariableNames.contains(score)) {
                Some(Errors.scalar(Seq(Message(
                  key = "unknownMovementScore",
                  details =
                    "guid" -> taskMovement.guid.toString,
                    "score" -> score
                ))))
              } else None
            }
          }.getOrElse {
            // The task movement's GUID must reference an actual movement.
            Some(Errors.scalar(Seq(Message(
              key = "unknownMovementGuid",
              details = "guid" -> taskMovement.guid.toString
            ))))
          }
        }

        errors = errors ++ taskMovements.flatMap(taskMovementErrors)

        // If there's a top-level score, it must be either 'time' or 'reps'.
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
    } yield result
  }

}
