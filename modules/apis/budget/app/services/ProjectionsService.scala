package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.validation._

import models.budget.Projection
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util.{UsersHelper, ResourceUnexpectedlyNotFound}

object ProjectionsService {

  def default()(implicit app: Application) = new ProjectionsService(UsersHelper.default())

}

class ProjectionsService(
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(records: Seq[ProjectionRecord])(implicit ec: ExecutionContext): Future[Seq[Projection]] = {
    val userGuids = records.map(_.createdByAccount)

    usersHelper.getUsers(userGuids).map { users =>
      records.map { record =>
        Projection(
          guid = record.guid,
          name = record.name,
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findProjections(
    ids: Option[Seq[Long]] = None
  )(implicit ec: ExecutionContext): Future[Seq[Projection]] = {
    var query = Projections.filter { r => r.id === r.id }

    ids.foreach { ids =>
      query = query.filter { _.id inSet ids }
    }

    database.run(query.result).flatMap(records2models)
  }

  def createProjection(
    name: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[Projection]] = {
    val projections = Projections.map { r =>
      (r.guid, r.name, r.createdByAccount)
    }.returning(Projections.map(_.id))

    val insert = projections += (UUID.randomUUID, name, accessToken.resourceOwner.guid)

    database.run(insert).flatMap { id =>
      findProjections(ids = Some(Seq(id))).map { projections =>
        projections.headOption.map { projection =>
          Validation.success(projection)
        }.getOrElse {
          throw new ResourceUnexpectedlyNotFound("Projection", id)
        }
      }
    }
  }

}
