package services.workouts

import db.workouts.DbConfigComponents

trait ServiceComponents {

  self: DbConfigComponents =>

  val movementsService = new MovementsService(dbConfig)

  val workoutsService = new WorkoutsService(dbConfig)

}
