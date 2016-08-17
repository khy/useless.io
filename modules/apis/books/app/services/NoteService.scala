package services.books

import java.util.UUID
import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import org.joda.time.DateTime
import slick.backend.DatabaseConfig
import io.useless.Message
import io.useless.account.Account
import io.useless.accesstoken.AccessToken
import io.useless.client.account.AccountClient
import io.useless.pagination._
import io.useless.validation._

import services.books.db.Driver
import db.{Notes, NoteRecord}
import models.books.{Edition, Note}

class NoteService(
  dbConfig: DatabaseConfig[Driver],
  accessTokenGuid: UUID
) {

  import dbConfig.db
  import dbConfig.driver.api._

  lazy val accountClient = {
    // TODO: Remove this once AccountClient is fixed.
    import play.api.Play.current
    AccountClient.instance(accessTokenGuid)
  }

  val tmpEdition = Edition(
    isbn = "JAH",
    title = "JAH",
    subtitle = None,
    authors = Seq.empty,
    pageCount = 5000,
    imageUrl = None,
    thumbnailUrl = None
  )

  def db2api(records: Seq[NoteRecord])(implicit ec: ExecutionContext): Future[Seq[Note]] = {
    val futAccounts = Future.sequence {
      records.map(_.createdByAccount).map(accountClient.getAccount(_))
    }.map { accounts =>
      accounts.filter(_.isDefined).map(_.get)
    }

    for {
      accounts <- futAccounts
    } yield {
      records.map { record =>
        val user = accounts.find { account =>
          account.guid == record.createdByAccount
        }.getOrElse {
          throw new ResourceUnexpectedlyNotFound("User", record.createdByAccount)
        }

        Note(record.guid, record.pageNumber, record.content, tmpEdition, user, new DateTime(record.createdAt))
      }
    }
  }

  private val paginationConfig = PaginationParams.defaultPaginationConfig.copy(
    validOrders = Seq("createdAt", "pageNumber"),
    defaultOrder = "createdAt"
  )

  def findNotes(
    guids: Option[Seq[UUID]],
    accountGuids: Option[Seq[UUID]],
    rawPaginationParams: RawPaginationParams
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[NoteRecord]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams, paginationConfig)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      // It's unclear to me why, but sortBy needs to go first.
      var query = Notes.sortBy { sort =>
        paginationParams.order match {
          case "pageNumber" => sort.pageNumber.desc
          case _ => sort.createdAt.desc
        }
      }

      guids.foreach { guids =>
        query = query.filter { note =>
          note.guid inSet guids
        }
      }

      accountGuids.foreach { accountGuids =>
        query = query.filter { note =>
          note.createdByAccount inSet accountGuids
        }
      }

      paginationParams match {
        case params: OffsetBasedPaginationParams[_] => {
          query = query.drop(params.offset)
        }
        case params: PrecedenceBasedPaginationParams[_] => {
          params.after.foreach { after =>
            val maxCreatedAt = Notes.filter(_.guid === after).
              map(_.createdAt).min.asColumnOf[Timestamp]

            query = query.filter(_.createdAt < maxCreatedAt)
          }
        }
      }

      query = query.take(paginationParams.limit)

      db.run(query.result).map { notes =>
        PaginatedResult.build(notes, paginationParams)
      }
    }
  }

  def addNote(
    isbn: String,
    pageNumber: Int,
    content: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[NoteRecord]] = {
    val edition = tmpEdition

    if (pageNumber < 1) {
      Future.successful {
        Validation.failure("pageNumber", "invalid-page-number",
          "specified-page-number" -> pageNumber.toString,
          "minimum-page-number" -> "1"
        )
      }
    } else if (pageNumber > edition.pageCount) {
      Future.successful {
        Validation.failure("pageNumber", "invalid-page-number",
          "specified-page-number" -> pageNumber.toString,
          "maximum-page-number" -> edition.pageCount.toString
        )
      }
    } else {
      insertNote(isbn, pageNumber, content, accessToken).flatMap { newGuid =>
        db.run(Notes.filter(_.guid === newGuid).result).map { records =>
          val record = records.headOption.getOrElse {
            throw new ResourceUnexpectedlyNotFound("Note", newGuid)
          }

          Validation.success(record)
        }
      }
    }
  }

  private def insertNote(
    isbn: String,
    pageNumber: Int,
    content: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[UUID] = {
    val projection = Notes.map { note =>
      (note.guid, note.isbn, note.pageNumber, note.content,
        note.createdByAccount, note.createdByAccessToken)
    }.returning(Notes.map(_.guid))

    val noteInsert = projection += (UUID.randomUUID, isbn, pageNumber, content,
      accessToken.resourceOwner.guid, accessToken.guid)

    db.run(noteInsert)
  }

}
