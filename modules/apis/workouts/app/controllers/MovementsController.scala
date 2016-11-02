package controllers.workouts

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.pagination.PaginationController
import io.useless.play.authentication.Authenticated

class MovementsController(
  authenticated: Authenticated
) extends Controller with PaginationController {

  def index = Action.async { implicit request =>
    Future.successful(Ok)
  }

  case class CreateData(
    name: String
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = authenticated.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(BadRequest(error.toString)),
      data => Future.successful(Ok)
    )
  }

}
