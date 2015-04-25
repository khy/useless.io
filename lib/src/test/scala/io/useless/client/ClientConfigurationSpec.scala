package io.useless.client

import org.scalatest.FunSpec
import org.scalatest.Matchers
import java.util.UUID
import play.api.Configuration
import com.typesafe.config.ConfigException

import io.useless.util.ConfigurationComponent

class ClientConfigurationSpec
  extends FunSpec
  with    Matchers
{

  trait ClientConfigurationProxy {

    self: ClientConfiguration =>

    def accessTokenGuid = clientConfiguration.accessTokenGuid

    def urlForPath(path: String) = clientConfiguration.urlForPath(path)

  }

  object EmptyClient extends ClientConfiguration with ClientConfigurationProxy {

    override lazy val configuration = Configuration.from(Map())

  }

  object BadClient extends ClientConfiguration with ClientConfigurationProxy {

    override lazy val configuration = Configuration.from(Map(
      "useless.client.accessTokenGuid" -> "invalid-guid",
      "useless.core.baseUrl" -> "http://useless.io/"
    ))

  }

  object Client extends ClientConfiguration with ClientConfigurationProxy {

    override lazy val configuration = Configuration.from(Map(
      "useless.client.accessTokenGuid" -> "3a65a664-89a0-4f5b-8b9e-f3226af0ff99",
      "useless.core.baseUrl" -> "http://useless.io"
    ))

  }

  describe ("ClientConfiguration#accessTokenGuid") {

    it ("should raise an error for an missing GUID") {
      a [ConfigException] should be thrownBy {
        EmptyClient.accessTokenGuid should be (UUID.fromString("3a65a664-89a0-4f5b-8b9e-f3226af0ff99"))
      }
    }

    it ("should raise an error for an invalid GUID") {
      a [IllegalArgumentException] should be thrownBy {
        BadClient.accessTokenGuid should be (UUID.fromString("3a65a664-89a0-4f5b-8b9e-f3226af0ff99"))
      }
    }


    it ("should return the configured, typed GUID") {
      Client.accessTokenGuid should be (UUID.fromString("3a65a664-89a0-4f5b-8b9e-f3226af0ff99"))
    }

  }

  describe ("ClientConfiguration#urlForPath") {

    it ("should raise an error if baseUrl is missing") {
      a [ConfigException] should be thrownBy {
        EmptyClient.urlForPath("/jah") should be ("http://useless.io/jah")
      }
    }

    it ("should not return URLs with double slashes") {
      BadClient.urlForPath("/jah") should be ("http://useless.io/jah")
    }

    it ("should return a URL with the specified path and configured base") {
      Client.urlForPath("/jah") should be ("http://useless.io/jah")
    }

  }

}
