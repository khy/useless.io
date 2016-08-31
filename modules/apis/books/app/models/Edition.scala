package models.books

import java.util.UUID
import play.api.libs.json._
import org.joda.time.LocalDate

case class Edition(
  isbn: String,
  title: String,
  subtitle: Option[String],
  authors: Seq[String],
  pageCount: Int,
  smallImageUrl: Option[String],
  largeImageUrl: Option[String],
  publisher: Option[String],
  publishedAt: Option[LocalDate],
  provider: Provider,
  providerId: Option[String]
)

object Edition {

  implicit val format = Json.format[Edition]

}
