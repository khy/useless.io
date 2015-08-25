package models.budget

import play.api.libs.json._
import io.useless.account.User
import io.useless.play.json.DateTimeJson._

import models.budget.util.KeyedJson

object JsonImplicits {

  private implicit val userWrites = Writes { user: User =>
    Json.obj(
      "guid" -> user.guid,
      "handle" -> user.handle,
      "name" -> user.name
    )
  }

  implicit val accountTypeFormat = KeyedJson.format(AccountType)
  implicit val accountWrites = Json.writes[Account]

}
