package io.useless.reactivemongo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection

import io.useless.util.configuration.Configuration

object MongoAccessor extends Configuration {

  def apply(uriConfigKey: String) = {
    configuration.getString(uriConfigKey).map { uri =>
      new MongoAccessor(uri)
    }.getOrElse {
      throw new MongoConfigException("Could not determine Mongo URI")
    }
  }

  class MongoConfigException(msg: String) extends RuntimeException(msg)

}

class MongoAccessor(uri: String) {

  private val parsedUri = MongoConnection.parseURI(uri).get

  lazy val driver = new MongoDriver

  lazy val connection: MongoConnection = driver.connection(parsedUri)

  lazy val db: DefaultDB = parsedUri.db.map { rawDb =>
    val db = connection(rawDb)

    parsedUri.authenticate.foreach { auth =>
      db.authenticate(auth.user, auth.password)(FiniteDuration(5, "seconds"))
    }

    db
  }.getOrElse {
    throw new MongoAccessor.MongoConfigException("Could not determine Mongo database")
  }

  def collection(name: String): BSONCollection = db(name)

}
