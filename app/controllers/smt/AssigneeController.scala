package controllers.smt

import app.Application._
import cats.data._
import cats.implicits._
import controllers._
import play.api.db.slick._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import slick.basic._
import slick.jdbc.JdbcProfile
import models.smt._
import javax.inject._
import scala.concurrent._

class AssigneeController @Inject()(
  val dbConfigProvider: DatabaseConfigProvider,
  cc: ControllerComponents,
  authenticated: UserAuthenticatedBuilder)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with SlickAssigneeDb
    with HasDatabaseConfigProvider[JdbcProfile] {

  implicit val assigneeFormat = Json.format[Assignee]
  implicit val serviceFormat = Json.format[Service]

  def list = authenticated.async { _ =>
    getAssignees.map(as => Ok(Json.toJson(as)))
  }

  def save = authenticated.async(parse.json) { implicit request =>
    (for {
        current <- parseBodyT[List[Assignee], Future]
        previous <- EitherT.right[DomainError](getAssignees)
        _ <- EitherT.right[DomainError](saveAssignees(previous, current))
      } yield Ok("assignees saved")).value
  }

  def listServices = authenticated.async {
    getServices.map(ss => Ok(Json.toJson(ss)))
  }

}
