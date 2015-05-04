package test.util

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.StaticQuery

import services.books.BaseService

object DbUtil extends BaseService {

  def clearDb() {
    val tables = Seq("authors")
    tables.foreach { truncateTable(_) }
  }

  def truncateTable(name: String) {
    database.withSession { implicit session =>
      StaticQuery.updateNA(s"TRUNCATE TABLE $name cascade").execute
    }
  }

}
