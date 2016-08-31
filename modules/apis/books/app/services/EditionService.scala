package services.books

import java.util.UUID
import java.sql.Date
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import org.postgresql.util.PSQLException
import slick.backend.DatabaseConfig
import org.joda.time.LocalDate

import services.books.db.{Driver, EditionCache, EditionCacheRecord}
import clients.books.EditionClient
import models.books.{Edition, Provider}

class EditionService(
  dbConfig: DatabaseConfig[Driver],
  editionClient: EditionClient
) {

  import dbConfig.db
  import dbConfig.driver.api._

  def getEditions(isbns: Seq[String])(implicit ec: ExecutionContext): Future[Seq[Edition]] = {
    val query = EditionCache.filter { record =>
      record.isbn inSet isbns
    }

    db.run(query.result).flatMap { records =>
      val cachedEditions = db2api(records)
      val missingIsbns = isbns.diff(records.map(_.isbn))

      if (missingIsbns.isEmpty) {
        Future.successful(cachedEditions)
      } else {
        editionClient.findByIsbn(missingIsbns).map { newEditions =>
          newEditions.foreach(cacheEdition)
          newEditions ++ cachedEditions
        }
      }
    }
  }

  private def db2api(records: Seq[EditionCacheRecord]): Seq[Edition] = {
    records.map { record =>
      Edition(
        isbn = record.isbn,
        title = record.title,
        subtitle = record.subtitle,
        authors = record.authors,
        pageCount = record.pageCount,
        largeImageUrl = record.largeImageUrl,
        smallImageUrl = record.smallImageUrl,
        publisher = record.publisher,
        publishedAt = record.publishedAt.map(new LocalDate(_)),
        provider = Provider(record.providerKey),
        providerId = record.providerId
      )
    }
  }

  /**
   * Atomically caches a single edition record. If no record exists for the
   * edition's ISBN, inserts a new record and returns the ID. If a record does
   * already exist, does nothing and returns a None.
   */
  private def cacheEdition(edition: Edition)(implicit ec: ExecutionContext): Future[Option[Long]] = {
    val insertProjection = EditionCache.map { r =>
      (r.isbn, r.title, r.subtitle, r.authors, r.pageCount, r.smallImageUrl,
        r.largeImageUrl, r.publisher, r.publishedAt, r.providerKey, r.providerId)
    }.returning(EditionCache.map(_.id))

    val editionCacheInsert = insertProjection += (edition.isbn, edition.title, edition.subtitle,
      edition.authors.toList, edition.pageCount, edition.smallImageUrl, edition.largeImageUrl,
      edition.publisher, edition.publishedAt, edition.provider.key, edition.providerId)

    db.run(editionCacheInsert).map { id =>
      Some(id)
    }.recover {
      case e: PSQLException if e.getMessage.contains("duplicate key value violates unique constraint") => {
        Logger.warn(s"cached edition already exists for ISBN [${edition.isbn}]")
        None
      }
    }
  }

}
