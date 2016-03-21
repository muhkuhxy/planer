package controllers

import java.time.LocalDate
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models._

object TerritoryController extends Controller with Security {
  def overview = Authenticated {
    NotFound("not implemented")
  }
}
