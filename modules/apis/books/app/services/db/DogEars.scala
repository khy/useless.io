package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.api._

case class DogEarRecord(
  guid: UUID,
  isbn: String,
  pageNumber: Int,
  note: Option[String],
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class DogEarTable(tag: Tag)
  extends Table[DogEarRecord](tag, "dog_ears")
  with AuditData[DogEarRecord]
{
  def guid = column[UUID]("guid")
  def isbn = column[String]("isbn")
  def pageNumber = column[Int]("page_number")
  def note = column[Option[String]]("note")

  def * = (guid, isbn, pageNumber, note, createdAt, createdByAccount,
    createdByAccessToken, deletedAt, deletedByAccount,
    deletedByAccessToken) <> (DogEarRecord.tupled, DogEarRecord.unapply)
}

object DogEars extends TableQuery(new DogEarTable(_))
