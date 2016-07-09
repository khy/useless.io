package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.api._

case class NoteRecord(
  guid: UUID,
  editionGuid: UUID,
  pageNumber: Int,
  content: String,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class NotesTable(tag: Tag)
  extends Table[NoteRecord](tag, "notes")
  with AuditData[NoteRecord]
{
  def guid = column[UUID]("guid")
  def editionGuid = column[UUID]("edition_guid")
  def pageNumber = column[Int]("page_number")
  def content = column[String]("content")

  def * = (guid, editionGuid, pageNumber, content, createdAt, createdByAccount,
    createdByAccessToken, deletedAt, deletedByAccount,
    deletedByAccessToken) <> (NoteRecord.tupled, NoteRecord.unapply)
}

object Notes extends TableQuery(new NotesTable(_))
