package io.useless.client

import java.util.UUID

import io.useless.util.{ Configuration, ConfigurationComponent}

trait ClientConfiguration
  extends DefaultClientConfigurationComponent
  with    Configuration
{

  def baseUrlConfigKey = "useless.baseUrl"

  def clientConfiguration = new DefaultClientConfiguration(baseUrlConfigKey)

}

trait ClientConfigurationComponent {

  def clientConfiguration: ClientConfiguration

  trait ClientConfiguration {

    def accessTokenGuid: UUID

    def urlForPath(path: String): String

  }

}

trait DefaultClientConfigurationComponent extends ClientConfigurationComponent {

  self: ConfigurationComponent =>

  class DefaultClientConfiguration(key: String) extends ClientConfiguration {

    lazy val accessTokenGuid = UUID.fromString(configuration.underlying.getString("useless.client.accessTokenGuid"))

    def urlForPath(path: String) = {
      val baseUrl = configuration.underlying.getString(key)
      val strippedBaseUrl = "/$".r.replaceAllIn(baseUrl, "")
      val strippedPath = "^/".r.replaceAllIn(path, "")
      strippedBaseUrl + "/" + strippedPath
    }

  }

}
