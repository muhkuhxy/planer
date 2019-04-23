package controllers.smt

import app.Application._
import cats.data._
import cats.implicits._
import controllers._
import java.time.LocalDate
import javax.inject._
import play.api.db.slick._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.smt._
import slick.basic._
import slick.jdbc.JdbcProfile
import scala.concurrent._

class PlanController @Inject()(
  val dbConfigProvider: DatabaseConfigProvider,
  cc: ControllerComponents,
  authenticated: UserAuthenticatedBuilder)(implicit ec: ExecutionContext)
    extends AbstractController(cc) with SlickPlanDb {
  import PlanController._

  def list = authenticated.async {
    listPlans.map { plans =>
      Ok(Json.toJson(plans))
    }
  }

  def show(id: Int) = authenticated.async {
    getPlan(id).map {
      case Some(plan) => Right(Ok(Json.toJson(plan)))
      case _ => Left(NoPlan)
    }
  }

  def create = authenticated(parse.json).async { implicit request =>
    {
      for {
        range <- parseBodyT[Range, Future]
        id <- EitherT.right[DomainError](createPlan(range.from, range.to))
      } yield Ok(routes.PlanController.show(id).absoluteURL)
    }.value
  }

  def save(id: Long) = authenticated(parse.json).async { implicit request =>
    {
      for {
        plan <- parseBodyT[PlanUpdateRequest, Future]
        _ <- EitherT.right[DomainError](savePlan(plan))
      } yield Ok("saved")
    }.value
  }

  def remove(id: Int) = authenticated.async {
    removePlan(id) map { _ => Ok("deleted")}
  }
}

object PlanController {

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
