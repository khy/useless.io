package services.books.db

import java.util.UUID
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.JsValue

import Driver.api._

case class EditionCacheRecord(
  id: Long,
  isbn: String,
  title: String,
  subtitle: Option[String],
  authors: List[String],
  pageCount: Int,
  smallImageUrl: Option[String],
  largeImageUrl: Option[String],
  publisher: Option[String],
  publishedAt: Option[LocalDate],
  providerKey: String,
  providerId: Option[String],
  createdAt: DateTime,
  deletedAt: Option[DateTime],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class EditionCacheTable(tag: Tag)
  extends Table[EditionCacheRecord](tag, "edition_cache")
{
  def id = column[Long]("id")
  def isbn = column[String]("isbn")
  def title = column[String]("title")
  def subtitle = column[Option[String]]("subtitle")
  def authors = column[List[String]]("authors")
  def pageCount = column[Int]("page_count")
  def smallImageUrl = column[Option[String]]("small_image_url")
  def largeImageUrl = column[Option[String]]("large_image_url")
  def publisher = column[Option[String]]("publisher")
  def publishedAt = column[Option[LocalDate]]("published_at")
  def providerKey = column[String]("provider_key")
  def providerId = column[Option[String]]("provider_id")
  def createdAt = column[DateTime]("created_at")
  def deletedAt = column[Option[DateTime]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, isbn, title, subtitle, authors, pageCount, smallImageUrl, largeImageUrl, publisher, publishedAt, providerKey, providerId, createdAt, deletedAt, deletedByAccount, deletedByAccessToken) <> (EditionCacheRecord.tupled, EditionCacheRecord.unapply)
}

object EditionCache extends TableQuery(new EditionCacheTable(_))
