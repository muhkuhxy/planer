package controllers

import java.time.LocalDate
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models._

object PlanerController extends Controller with Security {

  val assigness: AssigneeRepository = DefaultAssigneeRepository
  val plans: PlanRepository = DefaultPlanRepository

  case class Range(from: LocalDate, to: LocalDate)
  implicit val ldRead = new Reads[LocalDate] {
    def reads(json: JsValue) = JsSuccess(LocalDate.parse(json.as[String]))
  }
  implicit val SchedRead = Json.reads[Schedule]
  implicit val PlanRead = Json.reads[Plan]
  implicit val RangeRead = Json.reads[Range]

  def overview = Authenticated {
    Ok(views.html.overview(plans.list))
  }

  def show(id: Long) = Authenticated {
    Ok(views.html.plan(assigness.getAssignees, plans.find(id)))
  }

  def create = Authenticated(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[Range]
    result.fold(
      errors => {
        val readableErrors = JsError.toJson(errors)
        Logger.error(s"couldnt create plan: $errors")
        BadRequest(readableErrors)
      },
      range => {
        val id = plans.create(range.from, range.to)
        Ok(routes.PlanerController.show(id).absoluteURL)
      }
    )
  }

  def save(id: Long) = Authenticated(BodyParsers.parse.json) { request =>
    val result = request.body.validate[Plan]
    result.fold(
      errors => {
        val readableErrors = JsError.toJson(errors)
        Logger.error(s"couldnt save plan: $readableErrors")
        BadRequest(readableErrors)
      },
      plan => {
        plans.save(plan)
        Ok("saved")
      }
    )
  }

  def remove(id: Long) = Authenticated {
    plans.remove(id)
    Ok("deleted")
  }

}
