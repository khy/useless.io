package services.workouts

import db.workouts.DbConfigComponents

trait ServiceComponents {

  self: DbConfigComponents =>

  lazy val movementsService = new MovementsService(dbConfig)

  lazy val workoutsService = new WorkoutsService(dbConfig, movementsService)

}
