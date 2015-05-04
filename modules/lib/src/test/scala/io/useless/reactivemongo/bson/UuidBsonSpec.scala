package io.useless.reactivemongo.bson

import org.scalatest.FunSpec
import org.scalatest.Matchers

import java.util.UUID
import reactivemongo.bson.BSONDocument

class UuidBsonSpec
  extends FunSpec
  with    Matchers
{

  import UuidBson._

  describe ("DateTimeBson") {

    it ("should serialize and deserialize UUIDs seamlessly") {
      val uuid = UUID.randomUUID
      val bsonDocument = BSONDocument("uuid" -> uuid)
      bsonDocument.getAs[UUID]("uuid") should be (Some(uuid))
    }

  }

}
