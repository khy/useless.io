package services.books

import io.useless.client.account.AccountClientComponents

import services.books.db.DbConfigComponents
import clients.books.ClientComponents

trait ServiceComponents {

  self: DbConfigComponents with ClientComponents with AccountClientComponents =>

  lazy val editionService: EditionService = new EditionService(dbConfig, editionClient)

  lazy val noteService: NoteService = new NoteService(dbConfig, accountClient, editionService)

}
