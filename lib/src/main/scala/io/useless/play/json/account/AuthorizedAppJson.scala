package io.useless.play.json.account

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.account.{ App, AuthorizedApp }
import io.useless.play.json.UuidJson._

object AuthorizedAppJson {

  val reads: Reads[AuthorizedApp] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "app" \ "name").read[String] ~
    (__ \ "app" \ "url").read[String] ~
    (__ \ "app" \ "auth_redirect_url").read[String]
  ) { (guid: UUID, name: String, url: String, authRedirectUrl: String) =>
    App.authorized(guid, name, url, authRedirectUrl)
  }

  val writes = new Writes[AuthorizedApp] {
    def writes(app: AuthorizedApp): JsValue = {
      Json.obj(
        "guid" -> app.guid,
        "app" -> Json.obj(
          "name" -> app.name,
          "url" -> app.url,
          "auth_redirect_url" -> app.authRedirectUrl
        )
      )
    }
  }

}
