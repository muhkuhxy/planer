package controllers

import app.results._
import app.parsers._
import cats.data._
import cats.implicits._
import javax.inject._
import models._
import models.domainError._
import org.mindrot.jbcrypt.BCrypt
import play.api.db.slick._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent._

final case class LoginData(name: String, password: String)

class AuthenticationController @Inject()(
  cc: ControllerComponents,
  authenticated: UserAuthenticatedBuilder,
  val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with AuthenticationDb {

  implicit val loginReads = Json.reads[LoginData]

  def login = Action.async(parse.json) { implicit request =>
    {
      for {
        data <- parseBodyT[LoginData]
        _ <- EitherT { checkCredentials(data) }
      } yield loginUser(data.name)
    }.value
  }

  def user = authenticated { implicit request =>
    Ok(request.session("username"))
  }

  def logout = authenticated { implicit request =>
    Ok("loggedout").withNewSession
  }

  def checkCredentials(data: LoginData): Future[Either[DomainError, String]] =
    db.run {
      passwordFor(data.name).map(comparePasswords(data.password))
    }

  def comparePasswords(enteredPw: String)(maybePw: Option[String]): Either[DomainError, String] =
    maybePw.filter(hashedPw => BCrypt.checkpw(enteredPw, hashedPw))
      .toRight(InvalidCredentials)

  def loginUser(user: String)(implicit r: RequestHeader) =
    Ok(user).withSession(r.session + ("username" -> user))
}
