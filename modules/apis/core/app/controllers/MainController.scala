package controllers.core

import play.api.mvc._

object MainController extends Controller {

  def index = Action {
    Ok(views.html.main.index.render())
  }

}
