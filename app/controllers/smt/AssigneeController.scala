package controllers.smt

import app.Application._
import controllers.Security
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.smt._
import javax.inject._

class AssigneeController @Inject()(assignees: AssigneeRepository, val controllerComponents: ControllerComponents) extends Security {

  implicit val assigneeFormat = Json.format[Assignee]
  implicit val serviceFormat = Json.format[Service]

  def list = isAuthenticated { _ =>
    val json = Json.toJson(assignees.getAssignees)
    Ok(json)
  }

  def save = isAuthenticated(parse.json) { implicit request =>
    parseBody[List[Assignee]].map { result =>
      assignees.save(result)
      Ok("helpers saved")
    }
  }

  def listServices = isAuthenticated { _ =>
    Ok(Json.toJson(assignees.getServices))
  }

}
