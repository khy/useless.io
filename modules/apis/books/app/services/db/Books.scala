package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.api._

case class BookRecord(
  guid: UUID,
  title: String,
  authorGuid: UUID,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class BooksTable(tag: Tag)
  extends Table[BookRecord](tag, "books")
  with AuditData[BookRecord]
{
  def guid = column[UUID]("guid")
  def title = column[String]("title")
  def authorGuid = column[UUID]("author_guid")

  def * = (guid, title, authorGuid, createdAt, createdByAccount, createdByAccessToken,
    deletedAt, deletedByAccount, deletedByAccessToken) <> (BookRecord.tupled, BookRecord.unapply)
}

object Books extends TableQuery(new BooksTable(_))
