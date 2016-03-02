package controllers

import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models._

object AssigneeController extends Controller with Security {

  val assignees: AssigneeRepository = DefaultAssigneeRepository

  def list = Authenticated {
    Ok(views.html.assignees(assignees.getAssignees))
  }

  implicit val assigneeReads = Json.reads[Assignee]

  def save = Authenticated(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[List[Assignee]]
    result.fold(
      errors => {
        val readableErrors = JsError.toJson(errors)
        Logger.error(s"couldnt save helpers: $errors")
        BadRequest(readableErrors)
      },
      helpers => {
        assignees.save(helpers)
        Ok("helpers saved")
      }
    )
  }

}
