package controllers.territory

import controllers.Security
import java.time.{LocalDate, Period}
import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.territory._

class TerritoryController @Inject()(repo: TerritoryRepository) extends Controller with Security {

  implicit def localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
  def localDateDescOrdering: Ordering[LocalDate] = localDateOrdering.reverse

  def overview = Authenticated {
    Ok(views.html.territory.summary(Seq(), Seq(), Seq()))
  }

  def show(id: String) = Authenticated {
    repo.find(id) match {
      case Some(territory) => Ok(views.html.territory.single(territory))
      case None => NotFound(s"Gebiet $id nicht gefunden")
    }
  }


}
