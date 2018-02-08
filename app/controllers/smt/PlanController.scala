package controllers.smt

import app.Application._
import cats.implicits._
import controllers.Security
import java.time.LocalDate
import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.smt._

class PlanController @Inject()(assigness: AssigneeRepository, plans: PlanRepository, val controllerComponents: ControllerComponents) extends Security {

  case class Range(from: LocalDate, to: LocalDate)

  implicit val ldRead = new Reads[LocalDate] {
    def reads(json: JsValue) = JsSuccess(LocalDate.parse(json.as[String]))
  }
  implicit val optStringRead = new Reads[Option[String]] {
    def reads(json: JsValue) = json match {
      case JsString(s) if s.nonEmpty => JsSuccess(Some(s))
      case _ => JsSuccess(None)
    }
  }
  implicit val optIntRead = new Reads[Option[Int]] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => {
        JsSuccess(Some(n.intValue))
      }
      case _ => JsSuccess(None)
    }
  }
  implicit val serviceFmt = Json.writes[Service]
  implicit val assignemntRequestRead = Json.format[Assignment]
  implicit val schedRead = Json.writes[Schedule]
  implicit val planRead = Json.writes[Plan]
  implicit val rangeRead = Json.format[Range]
  implicit val partsRequestRead = Json.reads[PartsRequest]
  implicit val planUpdateRead = Json.reads[PlanUpdateRequest]
  implicit val planShellWrite = Json.writes[PlanShell]

  def list = isAuthenticated { _ =>
    Ok(Json.toJson(plans.list))
  }

  def show(id: Long) = isAuthenticated { _ =>
    plans.find(id) match {
      case Some(plan) => Right(Ok(Json.toJson(plan)))
      case _ => Left(NoPlan)
    }
  }

  def create = isAuthenticated(parse.json) { implicit request =>
    for {
      range <- parseBody[Range]
    } yield {
      Logger.debug(s"from $range")
      val id = plans.create(range.from, range.to)
      Ok(routes.PlanController.show(id).absoluteURL)
    }
  }

  def save(id: Long) = isAuthenticated(parse.json) { implicit request =>
    for {
      plan <- parseBody[PlanUpdateRequest]
    } yield {
      Logger.debug(s"saving $plan")
      plans.save(plan)
      Ok("saved")
    }
  }

  def remove(id: Long) = isAuthenticated { _ =>
    plans.remove(id)
    Ok("deleted")
  }

}
