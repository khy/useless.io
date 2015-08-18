package services.moofin

import scala.concurrent.Future
import org.joda.time.DateTime
import io.useless.validation._

import models.moofin.Meeting
import models.moofin.JsonImplicits._

class MeetingsService {

  def createMeeting(date: DateTime): Future[Validation[Meeting]] = {
    Future.successful(Validation.failure("jah", "der"))
  }

}
