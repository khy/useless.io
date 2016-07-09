package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.api._

case class AuthorRecord(
  guid: UUID,
  name: String,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class AuthorsTable(tag: Tag)
  extends Table[AuthorRecord](tag, "authors")
  with AuditData[AuthorRecord]
{
  def guid = column[UUID]("guid")
  def name = column[String]("name")

  def * = (guid, name, createdAt, createdByAccount, createdByAccessToken, deletedAt,
    deletedByAccount, deletedByAccessToken) <> (AuthorRecord.tupled, AuthorRecord.unapply)
}

object Authors extends TableQuery(new AuthorsTable(_))
