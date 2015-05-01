package io.useless.util.configuration

import java.util.UUID
import scala.util.{ Success, Failure }
import scala.util.control.Exception._
import play.api.{ Configuration => PlayConfig }
import com.typesafe.config.{ Config => TypesafeConfig, ConfigException }

import io.useless.util.Uuid

object RichConfiguration {

  implicit class RichPlayConfiguration(config: PlayConfig) {

    def getUuid(path: String): Option[UUID] = {
      catching(classOf[ConfigException.Missing]) opt config.underlying.getUuid(path)
    }

  }

  implicit class RichTypesafeConfig(config: TypesafeConfig) {

    def getUuid(path: String): UUID = {
      val value = config.getValue(path)

      value.unwrapped match {
        case string: String => Uuid.parseUuid(string) match {
          case Success(uuid) => uuid
          case Failure(e) => {
            throw new ConfigException.WrongType(value.origin, s"$path has type ${value.valueType} rather than UUID", e)
          }
        }
        case _ => throw new ConfigException.WrongType(value.origin, s"$path has type ${value.valueType} rather than UUID")
      }
    }

  }

}
