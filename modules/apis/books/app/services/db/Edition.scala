package services.books.db

import java.util.UUID
import java.sql.Timestamp

import Driver.simple._

private [services] case class Edition(
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

private [services] class Editions(tag: Tag)
  extends Table[Edition](tag, "editions")
  with AuditData[Edition]
{
  def guid = column[UUID]("guid")
  def bookGuid = column[UUID]("book_guid")
  def pageCount = column[Int]("page_count")

  def * = (guid, bookGuid, pageCount, createdAt, createdByAccount, createdByAccessToken,
    deletedAt, deletedByAccount, deletedByAccessToken) <> (Edition.tupled, Edition.unapply)
}

private [services] object Editions extends TableQuery(new Editions(_))
