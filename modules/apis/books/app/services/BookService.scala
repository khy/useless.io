package services.books

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import db._
import Driver.api._
import models.books.{ Book, Author, Edition }

object BookService extends BaseService {

  def db2api(records: Seq[BookRecord]): Future[Seq[Book]] = {
    val editionsQuery = Editions.filter(_.bookGuid inSet records.map(_.guid))
    val futEditionsMap = database.run(editionsQuery.result).flatMap { dbEditions =>
      EditionService.db2api(dbEditions).map { apiEditions =>
        dbEditions.groupBy(_.bookGuid).map { case (bookGuid, dbEditions) =>
          val _apiEditions = apiEditions.filter { apiEdition =>
            dbEditions.map(_.guid).contains(apiEdition.guid)
          }

          (bookGuid, _apiEditions)
        }
      }
    }

    val authorsQuery = Authors.filter(_.guid inSet records.map(_.authorGuid))
    val futAuthors = database.run(authorsQuery.result).flatMap { authors =>
      AuthorService.db2api(authors)
    }

    for {
      editionsMap <- futEditionsMap
      authors <- futAuthors
    } yield {
      records.map { record =>
        val _editions = editionsMap.get(record.guid).getOrElse(Seq.empty)

        val author = authors.find { author =>
          author.guid == record.authorGuid
        }.getOrElse {
          throw new ResourceUnexpectedlyNotFound("Author", record.authorGuid)
        }

        Book(record.guid, record.title, author, _editions)
      }
    }
  }

  def findBooks(
    guids: Option[Seq[UUID]] = None,
    titles: Option[Seq[String]] = None,
    editionGuids: Option[Seq[UUID]] = None
  ): Future[Seq[BookRecord]] = {
    var query: Query[BooksTable, BookRecord, Seq] = Books

    guids.foreach { guids =>
      query = query.filter { book =>
        book.guid inSet guids
      }
    }

    titles.foreach { titles =>
      titles.foreach { title =>
        query = query.filter { book =>
          (toTsVector(book.title) @@ toTsQuery(BaseService.scrubTsQuery(title)))
        }
      }
    }

    editionGuids.foreach { editionGuids =>
      val bookGuids = Editions.filter(_.guid inSet editionGuids).map(_.bookGuid)
      query = query.filter { book =>
        book.guid in bookGuids
      }
    }

    database.run(query.result)
  }

  def addBook(
    title: String,
    authorGuid: UUID,
    accessToken: AccessToken
  ): Future[BookRecord] = {
    database.run(Books.filter(_.title === title).result).flatMap { books =>
      books.headOption.map { book =>
        Future.successful(book)
      }.getOrElse {
        insertBook(title, authorGuid, accessToken).flatMap { newBookGuid =>
          findBooks(guids = Some(Seq(newBookGuid))).map { books =>
            books.headOption.getOrElse {
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

}
