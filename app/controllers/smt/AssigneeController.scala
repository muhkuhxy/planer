package controllers.smt

import controllers.Security
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.smt._
import javax.inject._

class AssigneeController @Inject()(assignees: AssigneeRepository) extends Controller with Security {

  def list = Authenticated {
    val json = Json.toJson(assignees.getAssignees)
    Ok(json)
  }

  implicit val assigneeReads = Json.format[Assignee]

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
      v => Assignee(v(0).as[Int],
        v(1).as[String],
        v(2).as[JsArray].value.map(lookupService(_)).toSet,
        parseEmail(v(3).as[String]))
    ).toList
  }

}
