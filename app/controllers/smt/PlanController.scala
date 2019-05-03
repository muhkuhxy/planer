package controllers.smt

import java.time.LocalDate

import app.Application._
import app.either._
import app.parsers._
import app.results._
import cats.data.EitherT
import cats.implicits._
import controllers._
import javax.inject._
import models.domainError._
import models.smt._
import play.api.libs.json._
import play.api.mvc._
import services.PlanService

import scala.concurrent._

object PlanController {

  case class Range(from: LocalDate, to: LocalDate)

  implicit val optStringRead = new Reads[Option[String]] {
    def reads(json: JsValue) = json match {
      case JsString(s) if s.nonEmpty => JsSuccess(Some(s))
      case _ => JsSuccess(None)
    }
  }

  implicit val optIntRead = new Reads[Option[Int]] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(Some(n.intValue))
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
  implicit val planShellWrite = Json.writes[PlanRow]
}

class PlanController @Inject()(
  service: PlanService,
  cc: ControllerComponents,
  authenticated: UserAuthenticatedBuilder)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {
  import PlanController._

  def list = authenticated.async {
    service.list.map { plans =>
      Ok(Json.toJson(plans))
    }
  }

  def show(id: Int) = authenticated.async {
    service.get(id).map {
      plan => Ok(Json.toJson(plan))
    }.value
  }

  def create = authenticated(parse.json).async { implicit request =>
    {
      for {
        range <- parseBodyT[Range]
        id <- service.create(range.from, range.to)
      } yield Ok(routes.PlanController.show(id).absoluteURL)
    }.value
  }

  def save(id: Long) = authenticated(parse.json).async { implicit request =>
    {
      for {
        plan <- parseBodyT[PlanUpdateRequest]
        _ <- right(service.save(plan))
      } yield Ok("saved")
    }.value
  }

  def remove(id: Int) = authenticated.async {
    service.delete(id) map { _ => Ok("deleted")}
  }
}
