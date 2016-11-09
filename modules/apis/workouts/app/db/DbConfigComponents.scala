package db.workouts

import slick.backend.DatabaseConfig
import play.api.db.slick.{SlickComponents, DbName}

trait DbConfigComponents {

  self: SlickComponents =>

  lazy val dbConfig: DatabaseConfig[Driver] = {
    api.dbConfig[Driver](DbName("workouts"))
  }

}
