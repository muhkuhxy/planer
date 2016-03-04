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
    val result = parseBody(request.body)
    assignees.save(result)
    Ok("helpers saved")
  }

  private def parseBody(body: JsValue) = {
    val serviceMap = body(0).as[JsArray].value.zipWithIndex.map({
        case (s, i) => i -> s.as[String]
    }).toMap
    def lookupService(id: JsValue): String = serviceMap(id.as[Int])
    body(1).as[JsArray].value.map(
      v => Assignee(v(0).as[String],
        v(1).as[JsArray].value.map(lookupService(_)).toSet)
    ).toList
  }

}
