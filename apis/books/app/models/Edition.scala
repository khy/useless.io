package models.books

import java.util.UUID
import play.api.libs.json._

case class Edition(
  guid: UUID,
  page_count: Int
)

object Edition {

  implicit val format = Json.format[Edition]

}
