import play.api.{Application, Logger}
import play.api.mvc.WithFilters
import com.typesafe.config.ConfigRenderOptions
import io.useless.play.filter._

import models.core.account.Account
import daos.account.AuthorizationDao

object Global
  extends WithFilters(
    new HttpsRedirectFilter,
    new AccessLogFilter,
    new RequestTimeFilter
  )
{

  override def onStart(app: Application) {
    Logger.info("Using this config:")
    Logger.info(app.configuration.underlying.root.render(ConfigRenderOptions.concise))

    Logger.info("Ensuring indexes...")
    Account.ensureIndexes()
    AuthorizationDao.instance.ensureIndexes()
  }

}
