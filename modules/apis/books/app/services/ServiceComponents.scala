package services.books

import services.books.db.DbConfigComponents
import clients.books.ClientComponents

trait ServiceComponents {

  self: DbConfigComponents with ClientComponents =>

  lazy val noteService: NoteService = new NoteService(dbConfig, accountClient)

}
