package io.useless.reactivemongo

import org.scalatest.{FunSuite, FunSpec}
import org.scalatest.Matchers

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.bson.BSONDocument

class MongoAccessTest
  extends FunSuite
  with    Matchers
  with    MongoAccess
{

  test ("MongoAccess#mongo provides access to the configured Mongo database") {
    val guid = UUID.randomUUID
    val doc = BSONDocument(
      "guid" -> guid.toString,
      "name" -> "jerp"
    )
    Await.result(mongo.collection("twonks").insert(doc), 5.seconds)

    val cursor = mongo.collection("twonks").
      find(BSONDocument("guid" -> guid.toString)).
      cursor[BSONDocument]

    val optDoc = Await.result(cursor.headOption, 5.seconds)
    optDoc.isDefined should be (true)

    val optName = optDoc.get.getAs[String]("name")
    optName.isDefined should be (true)
    optName.get should be ("jerp")
  }

}

trait AbstractMongoAccessSpec
  extends FunSpec
  with    Matchers
  with    MongoAccess

class MissingConfigMongoAccessSpec
  extends AbstractMongoAccessSpec
{

  override val mongoUri = None

  describe ("MongoAccess#mongo") {

    it ("should raise a MongoConfigException if a URI is not configured") {
      a [MongoConfigException] should be thrownBy { mongo }
    }

  }

}

class MissingDatabaseMongoAccessSpec
  extends AbstractMongoAccessSpec
{

  override val mongoUri = Some("mongodb://mongo.useless.io")

  describe ("MongoAccessor#db") {

    it ("should raise a MongoConfigException if a database is not configured") {
      a [MongoConfigException] should be thrownBy { mongo.db }
    }

  }

}

class AuthenticatedMongoAccessSpec
  extends AbstractMongoAccessSpec
{

  override val mongoUri = Some("mongodb://mongo.useless.io/useless")

  describe ("MongoAccessor#db") {

    it ("should return a DefaultDB with the configured database name") {
      mongo.db.name should be ("useless")
    }

  }

}
