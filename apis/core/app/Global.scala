import play.api.Application
import play.api.mvc.WithFilters
import io.useless.play.filter.{ AccessLogFilter, RequestTimeFilter }
import io.useless.util.Logger

import models.core.account.Account

object Global
  extends WithFilters(
    new AccessLogFilter,
    new RequestTimeFilter
  )
  with Logger
{

  override def onStart(app: Application) {
    logger.info("Ensuring indexes...")
    Account.ensureIndexes()
  }

}
