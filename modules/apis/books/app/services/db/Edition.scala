package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.api._

case class EditionRecord(
  guid: UUID,
  bookGuid: UUID,
  pageCount: Int,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class EditionsTable(tag: Tag)
  extends Table[EditionRecord](tag, "editions")
  with AuditData[EditionRecord]
{
  def guid = column[UUID]("guid")
  def bookGuid = column[UUID]("book_guid")
  def pageCount = column[Int]("page_count")

  def * = (guid, bookGuid, pageCount, createdAt, createdByAccount, createdByAccessToken,
    deletedAt, deletedByAccount, deletedByAccessToken) <> (EditionRecord.tupled, EditionRecord.unapply)
}

object Editions extends TableQuery(new EditionsTable(_))
