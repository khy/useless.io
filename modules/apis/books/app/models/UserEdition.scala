package models.books

import java.util.UUID
import play.api.libs.json._
import org.joda.time.LocalDate

case class UserEdition(
  edition: Edition
)

object UserEdition {

  implicit val format = Json.format[UserEdition]

}
