package controllers.workouts.old

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models.workouts.old._

class MeasuresController extends Controller {

  object DimensionJson {
    implicit val format = Json.format[Dimension]
  }

  def dimensions = Action {
    import DimensionJson.format
    Ok(Json.toJson(Dimension.values))
  }

  object UnitOfMeasureJson {
    import JsonImplicits.DimensionFormat
    implicit val format = Json.format[UnitOfMeasure]
  }

  def unitsOfMeasure = Action {
    import UnitOfMeasureJson.format
    Ok(Json.toJson(UnitOfMeasure.values))
  }

}
