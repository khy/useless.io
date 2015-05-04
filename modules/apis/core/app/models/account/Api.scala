package models.core.account

import io.useless.account.{ Api => UselessApi}

import mongo.Api._

class Api(
  account: Account,
  document: ApiDocument
) {

  lazy val toPublic = {
    UselessApi.public(account.guid, document.key)
  }

}
