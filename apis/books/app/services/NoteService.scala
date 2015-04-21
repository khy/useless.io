package services.books

import java.util.UUID
import java.sql.Timestamp
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.DateTime
import io.useless.ClientError
import io.useless.account.Account
import io.useless.accesstoken.AccessToken
import io.useless.client.account.AccountClient
import io.useless.pagination._

import db.Driver.simple._
import db.Notes
import models.books.Note

object NoteService extends BaseService {

  def getNote(guid: UUID): Future[Option[Note]] = {
    val futureDbNotes = withDbSession { implicit session =>
      Notes.filter { note => note.guid === guid }.list
    }

    futureDbNotes.flatMap(buildNotes(_)).map(_.headOption)
  }

  private val paginationConfig = PaginationParams.defaultPaginationConfig.copy(
    validOrders = Seq("created_at", "page_number")
  )

  def findNotes(
    accountGuids: Seq[UUID],
    rawPaginationParams: RawPaginationParams
  ): Future[Either[ClientError, PaginatedResult[Note]]] = {
    PaginationParams.build(rawPaginationParams, paginationConfig).fold(
      error => Future.successful(Left(error)),
      paginationParams => {

        val futureDbNotes = withDbSession { implicit session =>
          // It's unclear to me why, but sortBy needs to go first.
          var query = Notes.sortBy { sort =>
            val base = paginationParams.order match {
              case "page_number" => sort.pageNumber
              case _ => sort.createdAt
            }

            base.desc
          }

          if (accountGuids.nonEmpty) {
            query = query.filter { note =>
              note.createdByAccount inSet accountGuids
            }
          }

          paginationParams match {
            case params: OffsetBasedPaginationParams => {
              query = query.drop(params.offset)
            }
            case params: PrecedenceBasedPaginationParams => {
              params.after.foreach { after =>
                val maxCreatedAt = Notes.filter(_.guid === after).
                  map(_.createdAt).min.asColumnOf[Timestamp]

                query = query.filter(_.createdAt < maxCreatedAt)
              }
            }
          }

          query.take(paginationParams.limit).list
        }

        futureDbNotes.flatMap(buildNotes(_)).map { notes =>
          Right(PaginatedResult.build(notes, paginationParams))
        }
      }
    )
  }

  private val accountClient = AccountClient.instance

  private def buildNotes(dbNotes: Seq[db.Note]): Future[Seq[Note]] = {
    def getAccounts(guids: Seq[UUID]): Future[Seq[Account]] = Future.sequence {
      guids.map(accountClient.getAccount(_))
    }.map { accounts =>
      accounts.filter(_.isDefined).map(_.get)
    }

    for {
      books <- BookService.getBooksForEditions(dbNotes.map(_.editionGuid))
      accounts <- getAccounts(dbNotes.map(_.createdByAccount))
    } yield {
      dbNotes.map { dbNote =>
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

  def addNote(
    editionGuid: UUID,
    pageNumber: Int,
    content: String,
    accessToken: AccessToken
  ): Future[Either[ClientError, Note]] = {
    BookService.getBookForEdition(editionGuid).flatMap { optBook =>
      optBook.map { book =>
        val edition = book.editions.find(_.guid == editionGuid).getOrElse {
          throw new ResourceUnexpectedlyNotFound("Edition", editionGuid)
        }

        if (pageNumber < 1) {
          Future.successful {
            Left(ClientError("invalid-page-number",
              "specified-page-number" -> pageNumber.toString,
              "minimum-page-number" -> "1"
            ))
          }
        } else if (pageNumber > edition.page_count) {
          Future.successful {
            Left(ClientError("invalid-page-number",
              "specified-page-number" -> pageNumber.toString,
              "maximum-page-number" -> edition.page_count.toString
            ))
          }
        } else {
          // Notice: created_at is approximated to avoid another DB call
          insertNote(editionGuid, pageNumber, content, accessToken).map { noteGuid =>
            Right(Note(noteGuid, pageNumber, content, edition, book, accessToken.resourceOwner, DateTime.now))
          }
        }
      }.getOrElse {
        Future.successful {
          Left(ClientError("unknown-edition", "guid" -> editionGuid.toString))
        }
      }
    }
  }

  private def insertNote(
    editionGuid: UUID,
    pageNumber: Int,
    content: String,
    accessToken: AccessToken
  ): Future[UUID] = withDbSession { implicit session =>
    val projection = Notes.map { note =>
      (note.guid, note.editionGuid, note.pageNumber, note.content,
        note.createdByAccount, note.createdByAccessToken)
    }.returning(Notes.map(_.guid))

    projection += (UUID.randomUUID, editionGuid, pageNumber, content,
      accessToken.resourceOwner.guid, accessToken.guid)
  }

}
