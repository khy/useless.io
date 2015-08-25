package controllers.budget

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.DateTime
import io.useless.play.json.MessageJson.format

import controllers.budget.auth.Auth
import services.budget.MeetingsService
import models.budget.JsonImplicits._

object MeetingsController extends Controller {

  val meetingsService = new MeetingsService

  case class NewMeeting(date: DateTime)
  private implicit val nmReads = Json.reads[NewMeeting]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[NewMeeting].fold(
      error => Future.successful(Conflict),
      data => meetingsService.createMeeting(date = data.date).map { valMeeting =>
        valMeeting.fold(
          errors => Conflict(Json.toJson(errors)),
          meeting => Created(Json.toJson(meeting))
        )
      }
    )
  }

}
