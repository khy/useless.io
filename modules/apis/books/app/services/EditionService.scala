package services.books

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken
import io.useless.validation._

import db.Driver.api._
import db.{Books, Editions, EditionRecord, EditionsTable}
import models.books.Edition

object EditionService {

  def instance(): EditionService = {
    new EditionService
  }

}

class EditionService extends BaseService {

  def db2api(records: Seq[EditionRecord]): Future[Seq[Edition]] = Future.successful {
    records.map { edition =>
      Edition(edition.guid, edition.pageCount)
    }
  }

  def findEditions(
    guids: Option[Seq[UUID]] = None,
    bookGuids: Option[Seq[UUID]] = None
  ): Future[Seq[EditionRecord]] = {
    var query: Query[EditionsTable, EditionRecord, Seq] = Editions

    guids.foreach { guids =>
      query = query.filter { edition =>
        edition.guid inSet guids
      }
    }

    bookGuids.foreach { bookGuids =>
      query = query.filter { edition =>
        edition.bookGuid inSet bookGuids
      }
    }

    database.run(query.result)
  }

  def addEdition(
    bookGuid: UUID,
    pageCount: Int,
    accessToken: AccessToken
  ): Future[Validation[EditionRecord]] = {
    database.run(Books.filter(_.guid === bookGuid).result).flatMap { books =>
      books.headOption.map { book =>
        findEditions(bookGuids = Some(Seq(bookGuid))).flatMap { editions =>
          editions.find { edition =>
            edition.pageCount == pageCount
          }.map { edition =>
            Future.successful(Validation.success(edition))
          }.getOrElse {
            if (pageCount < 1) {
              Future.successful {
                Validation.failure("pageCount", "invalid-page-count",
                  "specified-page-count" -> pageCount.toString,
                  "minimum-page-count" -> "1"
                )
              }
            } else {
              insertEdition(bookGuid, pageCount, accessToken).flatMap { newEditionGuid =>
                findEditions(guids = Some(Seq(newEditionGuid))).map { editions =>
                  val edition = editions.headOption.getOrElse {
                    throw new ResourceUnexpectedlyNotFound("Edition", newEditionGuid)
                  }

                  Validation.success(edition)
                }
              }
            }
          }
        }
      }.getOrElse {
        Future.successful {
          Validation.failure("bookGuid", "unknown-book", "guid" -> bookGuid.toString)
        }
      }
    }
  }

  private def insertEdition(
    bookGuid: UUID,
    pageCount: Int,
    accessToken: AccessToken
  ): Future[UUID] = {
    val projection = Editions.map { edition =>
      (edition.guid, edition.bookGuid, edition.pageCount, edition.createdByAccount, edition.createdByAccessToken)
    }.returning(Editions.map(_.guid))

    val insertEdition = projection += (UUID.randomUUID, bookGuid, pageCount, accessToken.resourceOwner.guid, accessToken.guid)

    database.run(insertEdition)
  }

}
