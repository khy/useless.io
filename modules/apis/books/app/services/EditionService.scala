package services.books

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken
import io.useless.Message

import db.Driver.api._
import db.Editions
import models.books.Edition

object EditionService extends BaseService {

  def getEdition(guid: UUID): Future[Option[Edition]] = {
    val query = Editions.filter(_.guid === guid)

    database.run(query.result).map { records =>
      records.headOption.map { edition =>
        Edition(edition.guid, edition.pageCount)
      }
    }
  }

  def findEditions(bookGuid: UUID): Future[Seq[Edition]] = {
    val query = Editions.filter(_.bookGuid === bookGuid)

    database.run(query.result).map { records =>
      records.map { edition =>
        Edition(edition.guid, edition.pageCount)
      }
    }
  }

  def addEdition(
    bookGuid: UUID,
    pageCount: Int,
    accessToken: AccessToken
  ): Future[Either[Message, Edition]] = {
    BookService.getBook(bookGuid).flatMap { optBook =>
      optBook.map { book =>
        findEditions(bookGuid).flatMap { editions =>
          editions.find { edition =>
            edition.page_count == pageCount
          }.map { edition =>
            Future.successful(Right(edition))
          }.getOrElse {
            if (pageCount < 1) {
              Future.successful {
                Left(Message("invalid-page-count",
                  "specified-page-count" -> pageCount.toString,
                  "minimum-page-count" -> "1"
                ))
              }
            } else {
              insertEdition(bookGuid, pageCount, accessToken).flatMap { newEditionGuid =>
                getEdition(newEditionGuid).map { optEdition =>
                  val edition = optEdition.getOrElse {
                    throw new ResourceUnexpectedlyNotFound("Edition", newEditionGuid)
                  }

                  Right(edition)
                }
              }
            }
          }
        }
      }.getOrElse {
        Future.successful {
          Left(Message("unknown-book", "guid" -> bookGuid.toString))
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
