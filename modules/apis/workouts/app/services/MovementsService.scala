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
        Movement(
          guid = record.guid,
          name = record.name,
          variables = record.variables.map { variables =>
            Json.fromJson[Seq[Variable]](variables).fold(
              error => throw new InvalidState(s"Invalid variables JSON from DB [$error]"),
              variables => variables
            )
          }
        )
      }
    )
  }

  def addMovement(
    name: String,
    variables: Option[Seq[Variable]],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[MovementRecord]] = {
    val projection = Movements.map { r =>
      (r.guid, r.name, r.variables, r.createdByAccount, r.createdByAccessToken)
    }.returning(Movements.map(_.guid))

    val insert = projection += ((UUID.randomUUID, name, variables.map { v => Json.toJson(v) },
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
