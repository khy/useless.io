package services.workouts

import io.useless.client.account.AccountClientComponents

import db.workouts.DbConfigComponents

trait ServiceComponents {

  self: DbConfigComponents with AccountClientComponents =>

  lazy val movementsService = new MovementsService(dbConfig)

  lazy val workoutsService = new WorkoutsService(dbConfig)

  lazy val oldWorkoutsService = new old.WorkoutsService(dbConfig, accountClient)

}
