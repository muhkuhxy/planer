package controllers

import app.Application._
import cats.{Inject => _, _}
import cats.data._
import cats.implicits._
import javax.inject._
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.mvc._
import play.api.Logger
import play.api.i18n._
import play.api.mvc.Results._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.libs.json._
import slick.jdbc.JdbcProfile
import scala.concurrent._

class AuthenticationController @Inject()(
  cc: ControllerComponents,
  authenticated: UserAuthenticatedBuilder,
  val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with AuthenticationDb
    with I18nSupport
    with HasDatabaseConfigProvider[JdbcProfile] {
  import AuthenticationController._

  def login = Action.async(parse.json) { implicit request =>
    (for {
      data <- parseBodyT[LoginData, Future]
      _ <- checkCredentials(data)
    } yield loginUser(data.name)).value
  }

  def user = authenticated { implicit request =>
    Ok(request.session("username"))
  }

  def logout = authenticated { implicit request =>
    Ok("loggedout").withNewSession
  }

  def checkCredentials(data: LoginData): EitherT[Future, DomainError, String] =
    EitherT {
      db.run {
        passwordFor(data.name).map(comparePasswords(data.password))
      }
    }

  def comparePasswords(enteredPw: String)(maybePw: Option[String]): Either[DomainError, String] =
    maybePw.filter(hashedPw => BCrypt.checkpw(enteredPw, hashedPw))
      .toRight(InvalidCredentials)

  def loginUser(user: String)(implicit r: RequestHeader) =
    Ok(user)
      .withSession(r.session + ("username" -> user))
}

final case class LoginData(name: String, password: String)

object AuthenticationController {
  implicit val loginReads = Json.reads[LoginData]
}

