package support

import io.useless.util.mongo.MongoUtil

trait MongoHelper {

  def clearDb() = MongoUtil.clearDb("core.mongo.uri")

}
