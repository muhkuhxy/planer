package controllers.territory

import controllers.Security
import java.time.LocalDate
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.territory._

object TerritoryController extends Controller with Security {
  def overview = Authenticated {
    Ok(views.html.territory.inout(Seq(), Seq()))
  }
}
