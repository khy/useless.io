package models.haiku

import java.util.UUID
import play.api.libs.json._
import io.useless.account.User
import io.useless.play.json.DateTimeJson._

object JsonImplicits {

  implicit val userWrites = Writes { user: User =>
    Json.obj(
      "guid" -> user.guid,
      "handle" -> user.handle,
      "name" -> user.name
    )
  }

  implicit val shallowHaikuWrites = Json.writes[ShallowHaiku]
  implicit val haikuWrites = Json.writes[Haiku]

}
