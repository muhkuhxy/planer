package controllers.territory

import controllers.Security
import java.time.{LocalDate, Period}
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.territory._
import models.territory.vm._

object TerritoryController extends Controller with Security {

  implicit def localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
  def localDateDescOrdering: Ordering[LocalDate] = localDateOrdering.reverse

  def overview = Authenticated {
    Ok(views.html.territory.inout(Seq(), Seq(), Seq()))
  }
}
