package controllers

import app.Application._
import anorm._
import anorm.SqlParser._
import cats.{Inject => _, _}
import cats.data._
import cats.implicits._
import javax.inject._
import org.mindrot.jbcrypt.BCrypt
import play.api.mvc._
import play.api.Logger
import play.api.i18n._
import play.api.mvc.Results._
import play.api.mvc.Security._
import play.api.data._
import play.filters.csrf._
import play.api.data.Forms._
import play.api.db.Database
import play.api.libs.json._

trait Security extends BaseController {
  def getUserFromRequest(req: RequestHeader): Option[String] = req.session.get("username")
  def onUnauthorized(req: RequestHeader) = Unauthorized
  def isAuthenticated(f: => Request[AnyContent] => Result) = {
    Authenticated(getUserFromRequest, onUnauthorized) { user =>
      Action(request => f(request))
    }
  }
  def isAuthenticated[T](b: BodyParser[T])(f: => Request[T] => Result) = {
    Authenticated(getUserFromRequest, onUnauthorized) { user =>
      Action(b)(request => f(request))
    }
  }
}

object AuthenticationController {
  case class LoginData(name: String, password: String)

  implicit val loginReads = Json.reads[LoginData]
}

class AuthenticationController @Inject()(db: Database, val controllerComponents: ControllerComponents) extends Security with I18nSupport {
  import AuthenticationController._

  def checkCredentials(data: LoginData): Boolean = db.withConnection { implicit c =>
    SQL"select password from appuser where username = ${data.name}"
      .as(scalar[String].singleOpt)
      .map(hashedPw => BCrypt.checkpw(data.password, hashedPw))
      .getOrElse(false)
  }

  def login = Action(parse.json) { implicit request =>

    def startSession(user: String) = Ok(user).withSession(request.session + ("username" -> user))

    def verifyCredentials(isValid: Boolean, user: String): Either[DomainError, Result] =
      if (isValid) {
        Right(startSession(user))
      } else {
        Left(InvalidCredentials)
      }

    for {
      data <- parseBody[LoginData]
      valid = checkCredentials(data)
      result <- verifyCredentials(valid, data.name)
    } yield result
  }

  def user = isAuthenticated { implicit request =>
    Ok(request.session("username"))
  }

  def logout = isAuthenticated { implicit request =>
    Ok("loggedout").withNewSession
  }
}
