package models.budget

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.useless.account.User
import io.useless.play.json.DateTimeJson._

import models.budget.util.NamedEnumJson

object JsonImplicits {

  val userReads: Reads[User] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "handle").read[String] ~
    (__ \ "name").read[Option[String]]
  ) { (guid: UUID, handle: String, name: Option[String]) =>
    User(guid, handle, name)
  }

  val userWrites = new Writes[User] {
    def writes(user: User): JsValue = {
      Json.obj(
        "guid" -> user.guid,
        "handle" -> user.handle,
        "name" -> user.name
      )
    }
  }

  implicit val userFormats = Format(userReads, userWrites)

  implicit val accountTypeFormat = NamedEnumJson.keyFormat(AccountType)
  implicit val transactionClassFormat = NamedEnumJson.keyFormat(TransactionClass)
  implicit val accountFormat = Json.format[Account]
  implicit val projectionFormat = Json.format[Projection]
  implicit val transactionTypeFormat = Json.format[TransactionType]
  implicit val transactionFormat = Json.format[Transaction]

}
