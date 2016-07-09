package services.books

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import db.Driver.api._
import db.{ Books, Authors, Editions }
import models.books.{ Book, Author, Edition }

object BookService extends BaseService {

  def getBook(guid: UUID): Future[Option[Book]] = {
    queryBooks { case ((book, _), _) =>
      (book.guid === guid).?
    }.map(_.headOption)
  }

  def getBooksForEditions(editionGuids: Seq[UUID]): Future[Seq[Book]] = {
    queryBooks { case (_, edition) =>
      edition.map { edition =>
        edition.guid inSet editionGuids
      }
    }
  }

  def getBookForEdition(editionGuid: UUID): Future[Option[Book]] = {
    getBooksForEditions(Seq(editionGuid)).map(_.headOption)
  }

  def findBooks(
    titles: Option[Seq[String]]
  ): Future[Seq[Book]] = {
    queryBooks { case ((book, _), _) =>
      (toTsVector(book.title) @@ toTsQuery(BaseService.scrubTsQuery(titles.get.head))).?
    }
  }

  def addBook(
    title: String,
    authorGuid: UUID,
    accessToken: AccessToken
  ): Future[Book] = {
    queryBooks { case ((book, _), _) =>
      (book.title === title).?
    }.flatMap { books =>
      books.headOption.map { book =>
        Future.successful(book)
      }.getOrElse {
        insertBook(title, authorGuid, accessToken).flatMap { newBookGuid =>
          getBook(newBookGuid).map { optBook =>
            optBook.getOrElse {
              throw new ResourceUnexpectedlyNotFound("Book", newBookGuid)
            }
          }
        }
      }
    }
  }

  private def insertBook(
    title: String,
    authorGuid: UUID,
    accessToken: AccessToken
  ): Future[UUID] = {
    val projection = Books.map { book =>
      (book.guid, book.title, book.authorGuid, book.createdByAccount, book.createdByAccessToken)
    }.returning(Books.map(_.guid))

    val bookInsert = projection += (UUID.randomUUID, title, authorGuid, accessToken.resourceOwner.guid, accessToken.guid)

    database.run(bookInsert)
  }

  private case class BookRecord(
    bookGuid: UUID,
    bookTitle: String,
    authorGuid: UUID,
    authorName: String,
    editionGuid: Option[UUID],
    editionPageCount: Option[Int]
  )

  private def queryBooks(
    filter: (((Books, Authors), Rep[Option[Editions]])) => Rep[Option[Boolean]]
  ) = {
    val query = Books.join(Authors).on { case (book, author) =>
      book.authorGuid === author.guid
    }.joinLeft(Editions).on { case ((book, author), edition) =>
      book.guid === edition.bookGuid
    }.filter(filter).map { case ((book, author), edition) =>
      (book.guid, book.title, author.guid, author.name, edition.map(_.guid), edition.map(_.pageCount))
    }

    database.run(query.result).map { records =>
      val bookRecords = records.map(BookRecord.tupled)

      val editionsMap = bookRecords.groupBy(_.bookGuid).map { case (bookGuid, bookRecords) =>
        val editions = bookRecords.map { bookRecord =>
          for {
            guid <- bookRecord.editionGuid
            pageCount <- bookRecord.editionPageCount
          } yield Edition(guid, pageCount)
        }.filter(_.isDefined).map(_.get)

        (bookGuid, editions)
      }

      bookRecords.map { bookRecord =>
        val _editions = editionsMap.get(bookRecord.bookGuid).getOrElse {
          throw new ResourceUnexpectedlyNotFound("Edition", bookRecord.bookGuid, "book GUID")
        }
        val _author = Author(bookRecord.authorGuid, bookRecord.authorName)
        Book(bookRecord.bookGuid, bookRecord.bookTitle, _author, _editions)
      }
    }
  }

}
