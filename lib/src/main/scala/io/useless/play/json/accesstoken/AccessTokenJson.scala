package io.useless.play.json.accesstoken

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.accesstoken._

object AccessTokenJson {

  implicit val accessTokenReads = new Reads[AccessToken] {
    def reads(accessToken: JsValue): JsResult[AccessToken] = {
      AuthorizedAccessTokenJson.reads.reads(accessToken).
      orElse { PublicAccessTokenJson.reads.reads(accessToken) }
    }
  }

  implicit val accessTokenWrites = new Writes[AccessToken] {
    def writes(accessToken: AccessToken): JsValue = accessToken match {
      case accessToken: AuthorizedAccessToken =>
        AuthorizedAccessTokenJson.writes.writes(accessToken)
      case accessToken: PublicAccessToken =>
        PublicAccessTokenJson.writes.writes(accessToken)
    }
  }

}
