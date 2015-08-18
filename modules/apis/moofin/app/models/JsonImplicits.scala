package models.moofin

import play.api.libs.json._
import io.useless.account.User
import io.useless.play.json.DateTimeJson._

object JsonImplicits {

  private implicit val userWrites = Writes { user: User =>
    Json.obj(
      "guid" -> user.guid,
      "handle" -> user.handle,
      "name" -> user.name
    )
  }

  implicit val writes = Json.writes[Meeting]

}
