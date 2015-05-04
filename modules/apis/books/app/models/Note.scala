package models.books

import java.util.UUID
import java.sql.Timestamp
import play.api.libs.json._
import org.joda.time.DateTime
import io.useless.account.Account
import io.useless.play.json.account.AccountJson
import io.useless.play.json.DateTimeJson._

case class Note(
  guid: UUID,
  page_number: Int,
  content: String,
  edition: Edition,
  book: Book,
  created_by: Account,
  created_at: DateTime
)

object Note {

  implicit private val accountFormat =
    Format(AccountJson.accountReads, AccountJson.accountWrites)

  implicit val format = Json.format[Note]

}
