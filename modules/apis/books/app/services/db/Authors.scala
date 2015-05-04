package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.simple._

private [services] case class Author(
  guid: UUID,
  name: String,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

private [services] class Authors(tag: Tag)
  extends Table[Author](tag, "authors")
  with AuditData[Author]
{
  def guid = column[UUID]("guid")
  def name = column[String]("name")

  def * = (guid, name, createdAt, createdByAccount, createdByAccessToken, deletedAt,
    deletedByAccount, deletedByAccessToken) <> (Author.tupled, Author.unapply)
}

private [services] object Authors extends TableQuery(new Authors(_))
