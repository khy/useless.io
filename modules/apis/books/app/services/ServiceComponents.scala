package services.books

import io.useless.client.account.AccountClientComponents

import services.books.db.DbConfigComponents
import clients.books.ClientComponents

trait ServiceComponents {

  self: DbConfigComponents with ClientComponents with AccountClientComponents =>

  lazy val bookService: BookService = new BookService(dbConfig)

  lazy val dogEarService: DogEarService = new DogEarService(dbConfig, accountClient, editionService)

  lazy val editionService: EditionService = new EditionService(dbConfig, editionClient)

}
