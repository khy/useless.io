package test.util

import services.books.db.Driver.api._
import services.books.db.Authors
import slick.jdbc.StaticQuery

import services.books.BaseService

object DbUtil extends BaseService {

  def clearDb() {
    val query = Authors.filter { r => r.guid === r.guid }
    database.run(query.delete)
  }

}
