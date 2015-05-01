package io.useless.util.configuration

import play.api.Play
import play.api.{ Configuration => PlayConfig }
import com.typesafe.config.ConfigFactory

trait Configuration {

  lazy val configuration = Play.maybeApplication.map { application =>
    application.configuration
  }.getOrElse(fallbackConfig)

  private lazy val fallbackConfig = new PlayConfig(ConfigFactory.load())

}
