package services.core.social

import java.util.{Date, UUID}
import java.sql
import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import io.useless.typeclass.Identify
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

  def find(
    resourceApis: Option[Seq[String]],
    resourceTypes: Option[Seq[String]],
    resourceIds: Option[Seq[String]],
    accountGuids: Option[Seq[UUID]],
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[LikeRecord]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var query = Likes.filter(_.deletedAt.isEmpty)

      resourceApis.foreach { resourceApis =>
        query = query.filter(_.resourceApi inSet resourceApis)
      }

      resourceTypes.foreach { resourceTypes =>
        query = query.filter(_.resourceType inSet resourceTypes)
      }

      resourceIds.foreach { resourceIds =>
        query = query.filter(_.resourceId inSet resourceIds)
      }

      accountGuids.foreach { accountGuids =>
        query = query.filter(_.createdByAccount inSet accountGuids)
      }

      var pagedQuery = query.sortBy(_.createdAt.desc)

      pagedQuery = paginationParams match {
        case params: OffsetBasedPaginationParams[_] => pagedQuery.drop(params.offset)
        case params: PrecedenceBasedPaginationParams[_] => params.after.map { after =>
          pagedQuery.filter {
            _.createdAt < Likes.filter(_.guid === after).map(_.createdAt).min
          }
        }.getOrElse { pagedQuery }
      }

      pagedQuery = pagedQuery.take(paginationParams.limit)

      val futCount = database.run(query.length.result)
      val futLikeRecords = database.run(pagedQuery.result)

      for {
        count <- futCount
        likeRecords <- futLikeRecords
      } yield PaginatedResult.build(likeRecords, paginationParams, Some(count))
    }
  }

  implicit val likeAggregateIdentify = new Identify[LikeAggregate] {
    def identify(la: LikeAggregate) = {
      s"${la.resourceApi}/${la.resourceType}/${la.resourceId}"
    }
  }

  val aggregatePaginationConfig = PaginationParams.defaultPaginationConfig.copy(
    validStyles = Seq(OffsetBasedPagination, PageBasedPagination)
  )

  def aggregates(
    resourceApis: Option[Seq[String]],
    resourceTypes: Option[Seq[String]],
    resourceIds: Option[Seq[String]],
    accountGuids: Option[Seq[UUID]],
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[LikeAggregate]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams, aggregatePaginationConfig)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var query = Likes.filter(_.deletedAt.isEmpty)

      resourceApis.foreach { resourceApis =>
        query = query.filter(_.resourceApi inSet resourceApis)
      }

      resourceTypes.foreach { resourceTypes =>
        query = query.filter(_.resourceType inSet resourceTypes)
      }

      resourceIds.foreach { resourceIds =>
        query = query.filter(_.resourceId inSet resourceIds)
      }

      accountGuids.foreach { accountGuids =>
        query = query.filter(_.createdByAccount inSet accountGuids)
      }

      var aggQuery = query.groupBy { like =>
        (like.resourceApi, like.resourceType, like.resourceId)
      }.map { case ((resourceApi, resourceType, resourceId), group) =>
        (resourceApi, resourceType, resourceId, group.length)
      }

      var pagedQuery = aggQuery.sortBy { case (resourceApi, resourceType, resourceId, count) =>
        (resourceApi, resourceType, resourceId)
      }

      pagedQuery = paginationParams match {
        case params: OffsetBasedPaginationParams[_] => pagedQuery.drop(params.offset)
        case params: PrecedenceBasedPaginationParams[_] => params.after.map { after =>
          pagedQuery
        }.getOrElse { pagedQuery }
      }

      pagedQuery = pagedQuery.take(paginationParams.limit)

      val futCount = database.run(aggQuery.length.result)
      val futLikeAggregateRecords = database.run(pagedQuery.result)

      for {
        count <- futCount
        likeAggregateRecords <- futLikeAggregateRecords
      } yield {
        val likeAggregates = likeAggregateRecords.map { case (resourceApi, resourceType, resourceId, count) =>
          LikeAggregate(resourceApi, resourceType, resourceId, count)
        }

        PaginatedResult.build(likeAggregates, paginationParams, Some(count))
      }
    }
  }

  def create(
    resourceApi: String,
    resourceType: String,
    resourceId: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[LikeRecord]] = {
    getLike(resourceApi, resourceType, resourceId, accessToken).flatMap { optExistingLike =>
      optExistingLike.map { existingLike =>
        Future.successful(Validation.Success(existingLike))
      }.getOrElse {
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
  }

  def delete(
    resourceApi: String,
    resourceType: String,
    resourceId: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Option[LikeRecord]] = {
    getLike(resourceApi, resourceType, resourceId, accessToken).flatMap { optLike =>
      optLike.map { like =>
        val now = new sql.Timestamp((new Date).getTime)

        val query = Likes.filter { r =>
          r.id === like.id &&
          r.deletedAt.isEmpty
        }.map { like =>
          (like.deletedAt, like.deletedByAccount, like.deletedByAccessToken)
        }.update((Some(now), Some(accessToken.resourceOwner.guid), Some(accessToken.guid)))

        database.run(query).map { result =>
          if (result > 0) {
            Some(like)
          } else {
            throw new RuntimeException("Could not delete like " + like.id.toString)
          }
        }
      }.getOrElse {
        Future.successful(None)
      }
    }
  }

  private def getLike(
    resourceApi: String,
    resourceType: String,
    resourceId: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Option[LikeRecord]] = {
    val existingQuery = Likes.filter { r =>
      r.resourceApi === resourceApi &&
      r.resourceType === resourceType &&
      r.resourceId === resourceId &&
      r.createdByAccount === accessToken.resourceOwner.guid &&
      r.deletedAt.isEmpty
    }

    database.run(existingQuery.result).map(_.headOption)
  }

}
