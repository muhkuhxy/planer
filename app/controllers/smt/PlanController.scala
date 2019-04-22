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

class PlanController @Inject()(
  authenticated: UserAuthenticatedBuilder,
  plans: PlanRepository,
  val dbConfigProvider: DatabaseConfigProvider,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with SlickPlanDb
    with HasDatabaseConfigProvider[JdbcProfile] {
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

  def create = authenticated(parse.json) { implicit request =>
    parseBody[Range].map { range =>
      logger.debug(s"from $range")
//      val id = plans.create(range.from, range.to)
      Ok(routes.PlanController.show(3).absoluteURL)
    }
  }

  def save(id: Long) = authenticated(parse.json).async { implicit request =>
    (for {
      plan <- parseBodyT[PlanUpdateRequest, Future]
      _ <- EitherT.right[DomainError](savePlan(plan))
    } yield Ok("saved")).value
  }

  def remove(id: Long) = authenticated {
//    plans.remove(id)
    Ok("deleted")
  }
}

