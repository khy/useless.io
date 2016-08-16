package init

import play.api.{
  ApplicationLoader => PlayApplicationLoader,
  BuiltInComponents,
  BuiltInComponentsFromContext,
  Logger
}
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import com.typesafe.config.ConfigRenderOptions
import io.useless.play.filter._

import router.Routes

class ApplicationLoader extends PlayApplicationLoader {

  def load(context: Context) = {
    val applicationComponents = new ApplicationComponents(context)
    applicationComponents.application
  }

}

class ApplicationComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
{

  Logger.info(configuration.underlying.root.render(ConfigRenderOptions.concise))

  override lazy val httpFilters = Seq(
    CorsFilter,
    new HttpsRedirectFilter,
    new AccessLogFilter,
    new RequestTimeFilter
  )

  lazy val router: Router = new Routes(
    httpErrorHandler,
    core.Routes,
    books.Routes,
    haiku.Routes,
    budget.Routes
  )

}
