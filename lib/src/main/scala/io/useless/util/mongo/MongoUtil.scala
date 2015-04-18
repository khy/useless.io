package io.useless.util.mongo

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import io.useless.reactivemongo.MongoAccessor

object MongoUtil {

  def clearDb(uriConfigKey: String): Boolean = {
    val mongo = MongoAccessor(uriConfigKey)

    val clear = mongo.db.collectionNames.map { collectionNames =>
      collectionNames.
        filterNot { _.startsWith("system.") }.
        map       { mongo.db.collection(_).drop() }.
        map       { Await.result(_, 1.second) }.
        forall    { result => result }
    }

    Await.result(clear, 1.second)
  }

}
