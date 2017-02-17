package init.workouts

import play.api.{BuiltInComponents, BuiltInComponentsFromContext, Logger}
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import play.api.db.slick.SlickComponents
import play.api.libs.ws.ning.NingWSComponents
import io.useless.client.account.{AccountClientComponents, DefaultAccountClientComponents}
import io.useless.client.accesstoken.{AccessTokenClientComponents, DefaultAccessTokenClientComponents}
import io.useless.play.authentication.AuthenticatedComponents
import io.useless.util.configuration.RichConfiguration._

import workouts.Routes
import controllers.workouts._
import db.workouts.DbConfigComponents
import services.workouts.ServiceComponents

object ApplicationComponents {

  def router(context: Context) = {
    val applicationComponents = {
      new AbstractApplicationComponents(context)
        with DefaultAccountClientComponents
        with DefaultAccessTokenClientComponents
      {
        lazy val accessTokenClientBaseUrl = configuration.underlying.getString("useless.core.baseUrl")
        lazy val accessTokenClientAuthGuid = configuration.underlying.getUuid("workouts.accessTokenGuid")

        lazy val accountClientBaseUrl = configuration.underlying.getString("useless.core.baseUrl")
        lazy val accountClientAuthGuid = configuration.underlying.getUuid("workouts.accessTokenGuid")
      }
    }

    applicationComponents.workoutsRouter
  }

}

class AbstractApplicationComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
  with AuthenticatedComponents
  with NingWSComponents
  with SlickComponents
  with DbConfigComponents
  with ServiceComponents
{

  self: AccountClientComponents with AccessTokenClientComponents =>

  Logger.configure(context.environment)

  lazy val router: Router = workoutsRouter

  lazy val workoutsRouter = new Routes(
    httpErrorHandler,
    new WorkoutsController(authenticated, workoutsService),
    new MovementsController(authenticated, movementsService),
    new MeasuresController
  )

}
