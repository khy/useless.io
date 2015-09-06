package test.util

import services.books.BaseService
import services.books.db.Driver.api._
import services.books.db.Notes

object DbUtil extends BaseService {

  def clearDb() {
    val query = Notes.filter { r => r.guid === r.guid }
    database.run(query.delete)
  }

}
