package controllers.territory

import controllers.Security
import java.time.{LocalDate, Period}
import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.territory._

class TerritoryController @Inject()(territories: TerritoryRepository, friends: FriendRepository, val controllerComponents: ControllerComponents) extends Security {

  def overview = isAuthenticated { _ =>
    val (available, issued) = territories.summary
    Ok(views.html.territory.summary(available, issued))
  }

  def show(id: String) = isAuthenticated { _ =>
    territories.find(id) match {
      case Some(territory) => Ok(views.html.territory.single(territory, friends.all))
      case None => NotFound(s"Gebiet $id nicht gefunden")
    }
  }


}
