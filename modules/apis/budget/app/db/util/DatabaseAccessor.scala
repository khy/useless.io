package db.budget.util

import java.util.Date
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

object DatabaseAccessor {

  private lazy val database = Database.forConfig("db.budget")

}

trait DatabaseAccessor {

  protected lazy val database = DatabaseAccessor.database

}
