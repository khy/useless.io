package services.budget

import scala.concurrent.Future
import org.joda.time.DateTime
import io.useless.validation._

import models.budget.Meeting
import models.budget.JsonImplicits._

class MeetingsService {

  def createMeeting(date: DateTime): Future[Validation[Meeting]] = {
    Future.successful(Validation.failure("jah", "der"))
  }

}
