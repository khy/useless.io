package init.books

import play.api.{
  ApplicationLoader => PlayApplicationLoader,
  BuiltInComponents,
  BuiltInComponentsFromContext
}
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import play.api.libs.ws.ning.NingWSComponents
import play.api.db.slick.{SlickComponents, DbName}
import io.useless.util.configuration.RichConfiguration._

import books.Routes
import controllers.books._
import clients.books._
import services.books._

class ApplicationLoader extends PlayApplicationLoader {

  def load(context: Context) = ApplicationComponents.default(context).application

}

object  ApplicationComponents {

  def default(context: Context) = new ApplicationComponents(context)

  def router(context: Context) = default(context).booksRouter

}

class ApplicationComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
  with NingWSComponents
  with SlickComponents
{

  val accessTokenGuid = configuration.underlying.getUuid("books.accessTokenGuid")

  lazy val editionClient: EditionClient = new GoogleEditionClient(wsClient)

  lazy val dbConfig = api.dbConfig[db.Driver](DbName("books"))
  lazy val notesService: NoteService = new NoteService(dbConfig, accessTokenGuid)

  lazy val router: Router = booksRouter

  lazy val booksRouter = new Routes(
    httpErrorHandler,
    new Books,
    new Editions(editionClient),
    new Notes(notesService)
  )

}
