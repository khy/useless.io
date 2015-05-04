package io.useless.util.configuration

import java.util.UUID
import org.scalatest.FunSpec
import org.scalatest.Matchers
import play.api.{ Configuration => PlayConfig }
import com.typesafe.config.ConfigException

import io.useless.util.configuration.RichConfiguration._

class RichConfigurationSpec
  extends FunSpec
  with    Matchers
{

  describe ("RichPlayConfiguration.getUuid") {

    it ("should return a UUID instance for the specified path") {
      val configuration = PlayConfig.from(Map("uuid" -> "00000000-0000-0000-0000-000000000000"))
      configuration.getUuid("uuid") should be (Some(UUID.fromString("00000000-0000-0000-0000-000000000000")))
    }

    it ("should return None if the path doesn't exist") {
      val configuration = PlayConfig.from(Map("uuid" -> "00000000-0000-0000-0000-000000000000"))
      configuration.getUuid("guid") should be (None)
    }

    it ("should throw something") {
      val configuration = PlayConfig.from(Map("uuid" -> "not a UUID"))
      an [ConfigException.WrongType] should be thrownBy configuration.getUuid("uuid")
    }

  }

  describe ("RichTypesafeConfig.getUuid") {

    it ("should return a UUID instance for the specified path") {
      val config = PlayConfig.from(Map("uuid" -> "00000000-0000-0000-0000-000000000000")).underlying
      config.getUuid("uuid") should be (UUID.fromString("00000000-0000-0000-0000-000000000000"))
    }

    it ("should throw a ConfigException.Missing if the path doesn't exist") {
      val config = PlayConfig.from(Map("uuid" -> "00000000-0000-0000-0000-000000000000")).underlying
      an [ConfigException.Missing] should be thrownBy config.getUuid("guid")
    }

    it ("should throw a ConfigException.Wrong if the path exists, but is not a UUID") {
      val config = PlayConfig.from(Map("uuid" -> "not a UUID")).underlying
      an [ConfigException.WrongType] should be thrownBy config.getUuid("uuid")
    }

  }

}
