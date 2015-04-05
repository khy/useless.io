package io.useless.reactivemongo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection

import io.useless.util.Configuration

trait MongoAccess
  extends ConfigurableMongoAccessComponent

trait MongoAccessComponent {

  protected def mongo: MongoAccessor

  trait MongoAccessor {

    def driver = new MongoDriver

    def connection: MongoConnection

    def db: DefaultDB

    def collection(collectionName: String): BSONCollection =
      db(collectionName)

  }

}

trait ConfigurableMongoAccessComponent
  extends MongoAccessComponent
  with    Configuration
{

  def mongoUri = configuration.getString("mongo.uri")

  protected lazy val mongo = mongoUri.map {
    new ConfigurableMongoAccessor(_)
  }.getOrElse {
    throw new MongoConfigException("Could not determine Mongo URI")
  }

  class ConfigurableMongoAccessor(uri: String) extends MongoAccessor {

    private val parsedUri = MongoConnection.parseURI(uri).get

    lazy val connection = driver.connection(parsedUri)

    lazy val db = parsedUri.db.map { rawDb =>
      val db = connection(rawDb)

      parsedUri.authenticate.foreach { auth =>
        db.authenticate(auth.user, auth.password)(FiniteDuration(5, "seconds"))
      }

      db
    }.getOrElse {
      throw new MongoConfigException("Could not determine Mongo database")
    }

  }

  class MongoConfigException(msg: String) extends RuntimeException(msg)

}
