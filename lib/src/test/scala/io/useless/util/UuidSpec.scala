package io.useless.util

import org.scalatest.FunSpec
import org.scalatest.Matchers

import java.util.UUID
import scala.util.{ Success, Failure }

class UuidSpec
  extends FunSpec
  with    Matchers
{

  describe ("Uuid.parseUuid") {

    it ("should return a Success if a valid UUID string is specified") {
      val uuid = "3a65a664-89a0-4f5b-8b9e-f3226af0ff99"
      Uuid.parseUuid(uuid) should be (Success(UUID.fromString(uuid)))
    }

    it ("should return a Failure if an invalid UUID string is specified") {
      val uuid = "invalid-uuid"
      Uuid.parseUuid(uuid).getClass should be (classOf[Failure[UUID]])
    }

  }

}
