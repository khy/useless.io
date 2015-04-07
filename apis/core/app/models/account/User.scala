package models.core.account

import io.useless.account.{ User => UselessUser }

import mongo.User._

class User(
  account: Account,
  document: UserDocument
) {

  val email = document.email

  val handle = document.handle

  val name = document.name

  lazy val toPublic = {
    UselessUser.public(account.guid, document.handle, document.name)
  }

  lazy val toAuthorized = {
    UselessUser.authorized(account.guid, document.email, document.handle, document.name)
  }

}
