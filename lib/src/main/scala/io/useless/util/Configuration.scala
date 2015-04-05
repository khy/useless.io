package io.useless.util

import play.api.Play
import play.api.{ Configuration => PlayConfig }
import com.typesafe.config.ConfigFactory

trait Configuration
  extends DefaultConfigurationComponent

trait ConfigurationComponent {

  def configuration: PlayConfig

}

trait DefaultConfigurationComponent extends ConfigurationComponent {

  def configuration = Play.maybeApplication.map { application =>
    application.configuration
  }.getOrElse(fallbackConfig)

  private lazy val fallbackConfig = new PlayConfig(ConfigFactory.load())

}
