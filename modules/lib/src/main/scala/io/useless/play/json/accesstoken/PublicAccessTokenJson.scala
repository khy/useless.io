package io.useless.play.json.accesstoken

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.accesstoken._
import io.useless.account.Account
import io.useless.play.json.UuidJson._
import io.useless.play.json.account.AccountJson._
import ScopeJson._

object PublicAccessTokenJson {

  val reads: Reads[PublicAccessToken] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "resource_owner").read[Account] ~
    (__ \ "client").readNullable[Account] ~
    (__ \ "scopes").read[Seq[Scope]]
  ) {(guid: UUID, resourceOwner: Account, client: Option[Account], scopes: Seq[Scope]) =>
    AccessToken.public(guid, resourceOwner, client, scopes)
  }

  val writes = new Writes[PublicAccessToken] {
    def writes(accessToken: PublicAccessToken): JsValue ={
      Json.obj(
        "guid" -> accessToken.guid,
        "resource_owner" -> accessToken.resourceOwner,
        "client" -> accessToken.client,
        "scopes" -> accessToken.scopes
      )
    }
  }

}
