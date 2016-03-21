package controllers.smt

import controllers.Security
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.smt._

object AssigneeController extends Controller with Security {

  val assignees: AssigneeRepository = DefaultAssigneeRepository

  def list = Authenticated {
    Ok(views.html.smt.assignees(assignees.getAssignees))
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
    def parseEmail(em: String): Option[String] =
      if(em.nonEmpty) Some(em)
      else None
    body(1).as[JsArray].value.map(
      v => Assignee(v(0).as[String],
        v(1).as[JsArray].value.map(lookupService(_)).toSet,
        parseEmail(v(2).as[String]))
    ).toList
  }

}
