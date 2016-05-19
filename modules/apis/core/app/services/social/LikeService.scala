package services.core.social

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.core.account.Account
import models.core.social._
import db.core.social._

object LikeService {

  def instance(config: Configuration) = {
    new LikeService(
      database = Database.forConfig("db.core", config.underlying)
    )
  }

}

class LikeService(
  database: Database
) {

  def db2model(records: Seq[LikeRecord])(implicit ec: ExecutionContext): Future[Seq[Like]] = {
    val futUsers = Future.sequence(records.map { record =>
      Account.forGuid(record.createdByAccount).map { optAccount =>
        optAccount.flatMap(_.user)
      }
    }).map { optUsers =>
      optUsers.filter(_.isDefined).map(_.get).map(_.toPublic)
    }

    futUsers.map { users =>
      records.map { record =>
        Like(
          guid = record.guid,
          resourceApi = record.resourceApi,
          resourceType = record.resourceType,
          resourceId = record.resourceId,
          createdAt = new DateTime(record.createdAt.getTime),
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse {
            throw new RuntimeException("Could not find user " + record.createdByAccount.toString)
          }
        )
      }
    }
  }

  def create(
    resourceApi: String,
    resourceType: String,
    resourceId: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[LikeRecord]] = {
    val likes = Likes.map { r =>
      (r.guid, r.resourceApi, r.resourceType, r.resourceId, r.createdByAccount, r.createdByAccessToken)
    }.returning(Likes.map(_.id))

    val insert = likes += (UUID.randomUUID, resourceApi, resourceType, resourceId, accessToken.resourceOwner.guid, accessToken.guid)

    database.run(insert).flatMap { id =>
      database.run(Likes.filter(_.id === id).result).map { likes =>
        likes.headOption.map { like =>
          Validation.Success(like)
        }.getOrElse {
          throw new RuntimeException("Could not find like " + id.toString)
        }
      }
    }
  }

}
