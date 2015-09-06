package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.api._

case class Note(
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

class Notes(tag: Tag)
  extends Table[Note](tag, "notes")
  with AuditData[Note]
{
  def guid = column[UUID]("guid")
  def editionGuid = column[UUID]("edition_guid")
  def pageNumber = column[Int]("page_number")
  def content = column[String]("content")

  def * = (guid, editionGuid, pageNumber, content, createdAt, createdByAccount,
    createdByAccessToken, deletedAt, deletedByAccount,
    deletedByAccessToken) <> (Note.tupled, Note.unapply)
}

object Notes extends TableQuery(new Notes(_))
