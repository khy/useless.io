package init.books

import play.api.{BuiltInComponents, BuiltInComponentsFromContext, Logger}
import play.api.ApplicationLoader.Context
import play.api.inject.{Injector, SimpleInjector, NewInstanceInjector}
import play.api.routing.Router
import play.api.db.slick.{SlickComponents, DbName}
import play.api.libs.ws.ning.NingWSComponents

import books.Routes
import controllers.books._
import services.books.db.DbConfigComponents
import clients.books.{ClientComponents, ProdClientComponents}
import services.books.ServiceComponents

object ApplicationComponents {

  def router(context: Context) = {
    val applicationComponents = {
      new AbstractApplicationComponents(context)
        with ProdClientComponents
    }

    applicationComponents.booksRouter
  }

}

class AbstractApplicationComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
  with NingWSComponents
  with SlickComponents
  with DbConfigComponents
  with ServiceComponents
{

  self: ClientComponents =>

  Logger.configure(context.environment)

  lazy val router: Router = booksRouter

  lazy val booksRouter = new Routes(
    httpErrorHandler,
    new Books,
    new Editions(editionClient),
    new Notes(noteService)
  )

  override lazy val injector: Injector = {
    new SimpleInjector(NewInstanceInjector) + router + crypto + httpConfiguration +
      tempFileCreator + wsApi
  }

}
