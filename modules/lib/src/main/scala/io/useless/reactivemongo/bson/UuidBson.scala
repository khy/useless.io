package io.useless.reactivemongo.bson

import java.util.UUID
import reactivemongo.bson._

object UuidBson {

  implicit object UuidBsonReader extends BSONReader[BSONString, UUID] {
    def read(uuid: BSONString): UUID = UUID.fromString(uuid.value)
  }

  implicit object UuidBsonWriter extends BSONWriter[UUID, BSONString] {
    def write(uuid: UUID): BSONString = new BSONString(uuid.toString)
  }

}
