package models.books

import java.util.UUID
import play.api.libs.json._

case class Author(
  guid: UUID,
  name: String
)

object Author {

  implicit val format = Json.format[Author]

}
