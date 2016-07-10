package services.books

import java.util.UUID
import java.sql.Timestamp
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.DateTime
import io.useless.Message
import io.useless.account.Account
import io.useless.accesstoken.AccessToken
import io.useless.client.account.AccountClient
import io.useless.pagination._
import io.useless.validation._
import io.useless.util.configuration.Configuration
import io.useless.util.configuration.RichConfiguration._

import db.Driver.api._
import db.{Notes, NoteRecord}
import models.books.Note

object NoteService extends BaseService with Configuration {

  def db2api(records: Seq[NoteRecord]): Future[Seq[Note]] = {
    def getAccounts(guids: Seq[UUID]): Future[Seq[Account]] = Future.sequence {
      guids.map(accountClient.getAccount(_))
    }.map { accounts =>
      accounts.filter(_.isDefined).map(_.get)
    }

    for {
      books <- BookService.getBooksForEditions(records.map(_.editionGuid))
      accounts <- getAccounts(records.map(_.createdByAccount))
    } yield {
      records.map { dbNote =>
        val book = books.find { book =>
          book.editions.map(_.guid).contains(dbNote.editionGuid)
        }.getOrElse {
          throw new ResourceUnexpectedlyNotFound("Book", dbNote.editionGuid)
        }

        val edition = book.editions.find(_.guid == dbNote.editionGuid).getOrElse {
          throw new ResourceUnexpectedlyNotFound("Edition", dbNote.editionGuid)
        }

        val user = accounts.find { account =>
          account.guid == dbNote.createdByAccount
        }.getOrElse {
          throw new ResourceUnexpectedlyNotFound("User", dbNote.createdByAccount)
        }

        val createdAt = new DateTime(dbNote.createdAt)

        Note(dbNote.guid, dbNote.pageNumber, dbNote.content, edition, book, user, createdAt)
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
  ): Future[Validation[PaginatedResult[NoteRecord]]] = {
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

      database.run(query.result).map { notes =>
        PaginatedResult.build(notes, paginationParams)
      }
    }
  }

  lazy val accountClient = {
    val authGuid = configuration.underlying.getUuid("books.accessTokenGuid")
    AccountClient.instance(authGuid)
  }

  def addNote(
    editionGuid: UUID,
    pageNumber: Int,
    content: String,
    accessToken: AccessToken
  ): Future[Either[Message, NoteRecord]] = {
    BookService.getBookForEdition(editionGuid).flatMap { optBook =>
      optBook.map { book =>
        val edition = book.editions.find(_.guid == editionGuid).getOrElse {
          throw new ResourceUnexpectedlyNotFound("Edition", editionGuid)
        }

        if (pageNumber < 1) {
          Future.successful {
            Left(Message("invalid-page-number",
              "specified-page-number" -> pageNumber.toString,
              "minimum-page-number" -> "1"
            ))
          }
        } else if (pageNumber > edition.pageCount) {
          Future.successful {
            Left(Message("invalid-page-number",
              "specified-page-number" -> pageNumber.toString,
              "maximum-page-number" -> edition.pageCount.toString
            ))
          }
        } else {
          insertNote(editionGuid, pageNumber, content, accessToken).flatMap { newGuid =>
            database.run(Notes.filter(_.guid === newGuid).result).map { records =>
              val record = records.headOption.getOrElse {
                throw new ResourceUnexpectedlyNotFound("Note", newGuid)
              }

              Right(record)
            }
          }
        }
      }.getOrElse {
        Future.successful {
          Left(Message("unknown-edition", "guid" -> editionGuid.toString))
        }
      }
    }
  }

  private def insertNote(
    editionGuid: UUID,
    pageNumber: Int,
    content: String,
    accessToken: AccessToken
  ): Future[UUID] = {
    val projection = Notes.map { note =>
      (note.guid, note.editionGuid, note.pageNumber, note.content,
        note.createdByAccount, note.createdByAccessToken)
    }.returning(Notes.map(_.guid))

    val noteInsert = projection += (UUID.randomUUID, editionGuid, pageNumber, content,
      accessToken.resourceOwner.guid, accessToken.guid)

    database.run(noteInsert)
  }

}
