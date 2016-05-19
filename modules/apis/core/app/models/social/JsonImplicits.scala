package models.core.social

import java.util.UUID
import play.api.libs.json._
import io.useless.play.json.DateTimeJson._
import io.useless.play.json.account.UserJson._

object JsonImplicits {

  implicit val likeFormat = Json.format[Like]

}
