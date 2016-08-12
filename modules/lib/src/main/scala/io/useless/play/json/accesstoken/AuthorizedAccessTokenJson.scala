package io.useless.play.json.accesstoken

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.accesstoken._
import io.useless.account._
import io.useless.play.json.UuidJson._
import io.useless.play.json.account.AccountJson._
import ScopeJson._

object AuthorizedAccessTokenJson {

  val reads: Reads[AuthorizedAccessToken] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "authorization_code").read[UUID] ~
    (__ \ "resource_owner").read[Account] ~
    (__ \ "client").readNullable[Account] ~
    (__ \ "scopes").read[Seq[Scope]]
  ) {(guid: UUID, authorizationCode: UUID, resourceOwner: Account, client: Option[Account], scopes: Seq[Scope]) =>
    AccessToken.authorized(guid, authorizationCode, resourceOwner, client, scopes)
  }

  val writes = new Writes[AuthorizedAccessToken] {
    def writes(accessToken: AuthorizedAccessToken): JsValue ={
      Json.obj(
        "guid" -> accessToken.guid,
        "authorization_code" -> accessToken.authorizationCode,
        "resource_owner" -> accessToken.resourceOwner,
        "client" -> accessToken.client,
        "scopes" -> accessToken.scopes
      )
    }
  }

}
