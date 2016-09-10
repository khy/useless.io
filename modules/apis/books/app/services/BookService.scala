package services.books

import java.util.UUID
import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import org.joda.time.DateTime
import slick.backend.DatabaseConfig
import io.useless.Message
import io.useless.typeclass.Identify
import io.useless.account.Account
import io.useless.accesstoken.AccessToken
import io.useless.client.account.AccountClient
import io.useless.pagination._
import io.useless.validation._

import services.books.db.Driver
import models.books.Book
import db.{DogEars, EditionCache, EditionCacheTable, EditionCacheRecord}

class BookService(
  dbConfig: DatabaseConfig[Driver]
) {

  import dbConfig.db
  import dbConfig.driver.api._

  private val paginationConfig = PaginationParams.defaultPaginationConfig.copy(
    validOrders = Seq("createdAt", "pageNumber"),
    defaultOrder = "createdAt"
  )

  implicit val bookIdentify = new Identify[Book] {
    def identify(book: Book) = book.title
  }

  def findBooks(
    titles: Option[Seq[String]],
    rawPaginationParams: RawPaginationParams
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[Book]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams, paginationConfig)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var subQuery: Query[EditionCacheTable, EditionCacheRecord, Seq] = EditionCache

      titles.foreach { titles =>
        subQuery = subQuery.filter(_.title inSet titles)
      }

      val isbnsQuery = subQuery.groupBy(_.title).map { case (title, group) =>
        group.map(_.isbn).max
      }

      val query = EditionCache.filter { edition =>
        edition.isbn in isbnsQuery
      }

      db.run(query.result).map { case editions =>
        val books = editions.map { edition =>
          Book(
            edition.title,
            edition.subtitle,
            edition.authors,
            edition.smallImageUrl,
            edition.largeImageUrl
          )
        }

        PaginatedResult.build(books, paginationParams)
      }
    }
  }

}
