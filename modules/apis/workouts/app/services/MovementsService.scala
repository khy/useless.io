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

class MovementsService(
  dbConfig: DatabaseConfig[Driver]
) {

  import dbConfig.db
  import dbConfig.driver.api._

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

  def findMovements(
    guids: Option[Seq[UUID]] = None
  ): Future[Seq[MovementRecord]] = {
    var query: Query[MovementsTable, MovementRecord, Seq] = Movements

    guids.foreach { guids =>
      query = query.filter(_.guid inSet guids)
    }

    db.run(query.result)
  }

  def addMovement(
    movement: core.Movement,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[MovementRecord]] = {
    movement.variables.foreach { variables =>
      val dupNames = variables.groupBy(_.name).
        filter { case (_, variables) => variables.length > 1 }.
        map { case (name, _ ) => name }

      if (dupNames.size > 0) {
        val errors = Errors.scalar(dupNames.map { dupName =>
          Message(
            key = "duplicateVariableName",
            details = "name" -> dupName
          )
        }.toSeq)

        return Future.successful(Validation.failure(Seq(errors)))
      }
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
          Validation.success(movement)
        }.getOrElse {
          throw new ResourceNotFound("movement", guid)
        }
      }
    }
  }

}
