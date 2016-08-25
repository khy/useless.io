package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.http.QueryStringUtil._

import models.books.Book._

class Books extends Controller {

  def index = Action.async { request =>
    Future.successful(Ok(Json.toJson(Seq.empty)))
  }

}
