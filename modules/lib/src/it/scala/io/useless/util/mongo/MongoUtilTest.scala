package io.useless.util.mongo

import org.scalatest.FunSuite
import org.scalatest.Matchers
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.bson.BSONDocument

import io.useless.reactivemongo.MongoAccess

class MongoUtilTest
  extends FunSuite
  with    Matchers
  with    MongoAccess
{

  test ("MongoUtil.clearDb() drops non-system collections from the configured DB") {
    val guid = UUID.randomUUID
    val doc = BSONDocument("guid" -> guid.toString, "name" -> "blep")
    Await.result(mongo.collection("twonks").insert(doc), 5.seconds)
    Await.result(mongo.collection("poks").insert(doc), 5.seconds)

    Await.result(mongo.collection("twonks").find(BSONDocument("guid" -> guid.toString)).
      one[BSONDocument], 5.seconds) should not be (None)
    Await.result(mongo.collection("poks").find(BSONDocument("guid" -> guid.toString)).
      one[BSONDocument], 5.seconds) should not be (None)

    MongoUtil.clearDb()

    Await.result(mongo.collection("twonks").find(BSONDocument("guid" -> guid.toString)).
      one[BSONDocument], 5.seconds) should be (None)
    Await.result(mongo.collection("poks").find(BSONDocument("guid" -> guid.toString)).
      one[BSONDocument], 5.seconds) should be (None)
  }

}
