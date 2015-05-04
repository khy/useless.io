package io.useless.play.json.account

import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.account._
import io.useless.play.json.UuidJson._

object AccountJson {

  implicit val accountReads = new Reads[Account] {
    def reads(account: JsValue): JsResult[Account] = {
      PublicApiJson.reads.reads(account).
      orElse { AuthorizedAppJson.reads.reads(account) }.
      orElse { PublicAppJson.reads.reads(account) }.
      orElse { AuthorizedUserJson.reads.reads(account) }.
      orElse { PublicUserJson.reads.reads(account) }
    }
  }

  implicit val accountWrites = new Writes[Account] {
    def writes(account: Account): JsValue = account match {
      case api: PublicApi => PublicApiJson.writes.writes(api)
      case app: AuthorizedApp => AuthorizedAppJson.writes.writes(app)
      case app: PublicApp => PublicAppJson.writes.writes(app)
      case user: AuthorizedUser => AuthorizedUserJson.writes.writes(user)
      case user: PublicUser => PublicUserJson.writes.writes(user)
    }
  }

}
