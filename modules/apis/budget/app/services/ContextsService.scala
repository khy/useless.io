package services.budget

import java.util.{Date, UUID}
import java.sql.Timestamp
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.{LocalDate, DateTime}
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget._
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util._

object ContextsService {

  def default()(implicit app: Application) = new ContextsService(
    UsersHelper.default()
  )

}

class ContextsService(
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(
    records: Seq[ContextRecord]
  )(implicit ec: ExecutionContext): Future[Seq[Context]] = {
    val contextUsersQuery = ContextUsers.filter { contextUser =>
      contextUser.contextId.inSet(records.map(_.id)) &&
      contextUser.deletedAt.isEmpty
    }

    val futContextUserRecords = database.run(contextUsersQuery.result)

    val futUsers = futContextUserRecords.flatMap { contextUserRecords =>
      val auditUserGuids = records.flatMap { record =>
        record.deletedByAccount.toSeq :+ record.createdByAccount
      }

      val contextUserGuids = contextUserRecords.map(_.userGuid)

      val userGuids = (auditUserGuids ++ contextUserGuids).distinct
      usersHelper.getUsers(userGuids)
    }

    for {
      contextUserRecords <- futContextUserRecords
      users <- futUsers
    } yield {
      records.map { record =>
        Context(
          guid = record.guid,
          name = record.name,
          users = users.filter { user =>
            val userGuids = contextUserRecords.filter(_.contextId == record.id).map(_.userGuid)
            userGuids.contains(user.guid)
          },
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt),
          deletedBy = users.find(_.guid == record.deletedByAccount),
          deletedAt = record.deletedAt.map { deletedAt => new DateTime(deletedAt) }
        )
      }
    }
  }

  def findContexts(
    ids: Option[Seq[Long]],
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[ContextRecord]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.future(valPaginationParams) { paginationParams =>
      var query = Contexts.filter(_.deletedAt.isEmpty)

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
      }

      database.run(query.result).map { contextRecords =>
        PaginatedResult.build(contextRecords, paginationParams, None)
      }
    }
  }

  def createContext(
    name: String,
    userGuids: Seq[UUID],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[ContextRecord]] = {
    val insertProjection = Contexts.map { r =>
      (r.guid, r.name, r.createdByAccount, r.createdByAccessToken)
    }.returning(Contexts.map(_.id))

    val contextInsert = insertProjection += ((UUID.randomUUID, name, accessToken.resourceOwner.guid, accessToken.guid))

    database.run(contextInsert).flatMap { contextId =>
      val insertProjection = ContextUsers.map { r =>
        (r.contextId, r.userGuid, r.createdByAccount, r.createdByAccessToken)
      }

      val contextUsersInsert = insertProjection ++= userGuids.map { userGuid =>
        (contextId, userGuid, accessToken.resourceOwner.guid, accessToken.guid)
      }

      database.run(contextUsersInsert).flatMap { _ =>
        findContexts(ids = Some(Seq(contextId))).map { result =>
          result.map(_.items.headOption) match {
            case Validation.Success(Some(context)) => Validation.success(context)
            case _ => throw new ResourceUnexpectedlyNotFound("Context", contextId)
          }
        }
      }
    }
  }

}
