package controllers.smt

import app.either._
import app.parsers._
import app.results._
import cats.implicits._
import controllers._
import javax.inject._
import models.smt._
import play.api.db.slick._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent._

class AssigneeController @Inject()(
  cc: ControllerComponents,
  val dbConfigProvider: DatabaseConfigProvider,
  authenticated: UserAuthenticatedBuilder)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with SlickAssigneeDb {

  implicit val assigneeFormat = Json.format[Assignee]
  implicit val serviceFormat = Json.format[Service]

  def list = authenticated.async { _ =>
    getAssignees.map {
      assignees => Ok(Json.toJson(assignees))
    }
  }

  def save = authenticated.async(parse.json) { implicit request =>
    {
      for {
        current <- parseBodyT[List[Assignee]]
        previous <- right(getAssignees)
        _ <- right(saveAssignees(previous, current))
      } yield Ok("assignees saved")
    }.value
  }

  def listServices = authenticated.async {
    getServices.map {
      services => Ok(Json.toJson(services))
    }
  }
}
