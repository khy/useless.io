package models.core.account

import io.useless.account.{ App => UselessApp }

import mongo.App._

class App(
  account: Account,
  document: AppDocument
) {

  lazy val toPublic = {
    UselessApp.public(account.guid, document.name, document.url)
  }

  lazy val toAuthorized = {
    UselessApp.authorized(account.guid, document.name, document.url, document.authRedirectUrl)
  }

}
