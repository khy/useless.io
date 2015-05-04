package support

import io.useless.util.mongo.MongoUtil
import io.useless.reactivemongo.MongoAccessor

object MongoHelper {

  private lazy val mongo = MongoAccessor("core.mongo.uri")

  def clearDb() = MongoUtil.clearDb(mongo)

}
