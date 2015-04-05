package io.useless.util

import java.util.UUID
import scala.util.Try

object Uuid {

  def parseUuid(uuid: String): Try[UUID] = Try {
    UUID.fromString(uuid)
  }

}
