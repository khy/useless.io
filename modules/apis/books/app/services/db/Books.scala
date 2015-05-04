package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.simple._

private [services] case class Book(
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

private [services] class Books(tag: Tag)
  extends Table[Book](tag, "books")
  with AuditData[Book]
{
  def guid = column[UUID]("guid")
  def title = column[String]("title")
  def authorGuid = column[UUID]("author_guid")

  def * = (guid, title, authorGuid, createdAt, createdByAccount, createdByAccessToken,
    deletedAt, deletedByAccount, deletedByAccessToken) <> (Book.tupled, Book.unapply)
}

private [services] object Books extends TableQuery(new Books(_))
