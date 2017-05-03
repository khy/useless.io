package services.workouts.old

import io.useless.client.account.AccountClientComponents

import db.workouts.DbConfigComponents

trait ServiceComponents {

  self: DbConfigComponents with AccountClientComponents =>

  lazy val movementsService = new MovementsService(dbConfig)

  lazy val workoutsService = new WorkoutsService(dbConfig, movementsService, accountClient)

}
