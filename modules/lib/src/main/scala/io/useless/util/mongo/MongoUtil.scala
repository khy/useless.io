package io.useless.util.mongo

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import io.useless.reactivemongo.MongoAccessor

object MongoUtil {

  def clearDb(uriConfigKey: String): Boolean = {
    clearDb(MongoAccessor(uriConfigKey))
  }

  def clearDb(mongo: MongoAccessor): Boolean = {
    val clear = mongo.db.collectionNames.map { collectionNames =>
      collectionNames.
        filterNot { _.startsWith("system.") }.
        map       { mongo.db.collection(_).drop() }.
        map       { Await.result(_, 1.second) }.
        forall    { result => result }
    }

    Await.result(clear, 10.second)
  }

}
