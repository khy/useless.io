package models.haiku

import io.useless.account.User
import models.haiku.mongo.HaikuMongo.HaikuDocument

class Haiku(
  val createdBy: User,
  document: HaikuDocument
) {

  val guid = document.guid

  val lines = document.lines

  val createdAt = document.createdAt

}
