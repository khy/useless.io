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
import dsl.workouts.validate.MovementValidator

class MovementsService(
  dbConfig: DatabaseConfig[Driver]
) {

  import dbConfig.db
  import dbConfig.driver.api._


  import dbConfig.driver.api.playJsonColumnExtensionMethods

  def db2api(records: Seq[MovementRecord]): Future[Seq[Movement]] = {
    Future.successful(
      records.map { record =>
        Json.fromJson[core.Movement](record.json).fold(
          error => throw new InvalidState(s"Invalid movement JSON from DB [$error]"),
          movement => Movement(
            guid = record.guid,
            name = movement.name,
            variables = movement.variables,
            createdAt = record.createdAt,
            createdByAccount = record.createdByAccount,
            deletedAt = record.deletedAt,
            deletedByAccount = record.deletedByAccount
          )
        )
      }
    )
  }

  private val paginationConfig = PaginationParams.defaultPaginationConfig.copy(
    validOrders = Seq("createdAt", "name"),
    defaultOrder = "name"
  )

  def findMovements(
    names: Option[Seq[String]],
    rawPaginationParams: RawPaginationParams
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[Movement]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams, paginationConfig)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var query: Query[MovementsTable, MovementRecord, Seq] = Movements

      names.foreach { names =>
        names.map { name =>
          query = query.filter { movement =>
            movement.json.+>>("name").toLowerCase like s"%${name.toLowerCase}%"
          }
        }
      }

      val pagedQuery = query.sortBy { movement =>
        paginationParams.order match {
          case "name" => movement.json.+>>("name").asc
        }
      }.take(paginationParams.limit)

      db.run(pagedQuery.result).flatMap { case movementRecords =>
        db2api(movementRecords).map { movements =>
          PaginatedResult.build(movements, paginationParams)
        }
      }
    }
  }

  private [workouts] def getMovementsByGuid(guids: Seq[UUID]): Future[Seq[MovementRecord]] = {
    val query = Movements.filter(_.guid inSet guids)
    db.run(query.result)
  }

  def addMovement(
    movement: core.Movement,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Either[Seq[(JsPath, Seq[ValidationError])], MovementRecord]] = {
    val errors = MovementValidator.validateMovement(movement)

    if (errors.length > 0) {
      return Future.successful(Left(errors))
    }

    val projection = Movements.map { r =>
      (r.guid, r.schemaVersionMajor, r.schemaVersionMinor, r.json,
       r.createdByAccount, r.createdByAccessToken)
    }.returning(Movements.map(_.guid))

    val insert = projection += ((UUID.randomUUID, 1, 0, Json.toJson(movement),
      accessToken.resourceOwner.guid, accessToken.guid))

    db.run(insert).flatMap { guid =>
      val query = Movements.filter(_.guid === guid)
      db.run(query.result).map { movements =>
        movements.headOption.map { movement =>
          Right(movement)
        }.getOrElse {
          throw new ResourceNotFound("movement", guid)
        }
      }
    }
  }

}
