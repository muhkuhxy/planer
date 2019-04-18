package controllers.territory

import controllers.UserAuthenticatedBuilder
import java.time.{LocalDate, Period}
import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.territory._

class TerritoryController @Inject()(
  authenticated: UserAuthenticatedBuilder,
  territories: TerritoryRepository,
  friends: FriendRepository,
  cc: ControllerComponents)
    extends AbstractController(cc) {

  def overview = authenticated {
    val (available, issued) = territories.summary
    Ok(views.html.territory.summary(available, issued))
  }

  def show(id: String) = authenticated {
    territories.find(id) match {
      case Some(territory) => Ok(views.html.territory.single(territory, friends.all))
      case None => NotFound(s"Gebiet $id nicht gefunden")
    }
  }


}
