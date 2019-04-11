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
import play.api.db.slick._
import play.api.db.Database
import play.api.libs.json._
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.basic._
import scala.concurrent._

final case class UserRow(name: String, password: String)

trait AuthenticationAccess {

  lazy val users = TableQuery[UserTable]

  def passwordFor(name: String): DBIO[Option[String]] =
    users.filter(_.name === name).result.headOption

  class UserTable(tag: Tag) extends Table[String](tag, "appuser") {
    def name = column[String]("username")
    def password = column[String]("password")

    def * = password
  }
}

trait Security extends BaseController {
  def getUserFromRequest(req: RequestHeader): Option[String] =
    req.session.get("username")

  def onUnauthorized(req: RequestHeader) = Unauthorized

  def isAuthenticated[A](f: => Request[AnyContent] => Result) = {
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

class AuthenticationController @Inject()(
  cc: ControllerComponents,
  val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with AuthenticationAccess
    with Security
    with I18nSupport
    with HasDatabaseConfigProvider[JdbcProfile] {
  import AuthenticationController._

  def checkCredentials(enteredPw: String)(maybePw: Option[String]): Either[DomainError, String] =
    maybePw.filter(hashedPw => BCrypt.checkpw(enteredPw, hashedPw))
      .toRight(InvalidCredentials)

  def wrap[A](either: Either[DomainError, A]): EitherT[Future, DomainError, A] =
    EitherT.fromEither(either)

  def readAndCheckCredentials(data: LoginData): EitherT[Future, DomainError, String] =
    EitherT(db.run(passwordFor(data.name).map { checkCredentials(data.password) }))

  def login = Action.async(parse.json) { implicit request =>

    def startSession(user: String) =
      Ok(user)
        .withSession(request.session + ("username" -> user))

    for {
      data <- parseBody[LoginData].toT[Future]
      valid <- readAndCheckCredentials(data)
    } yield startSession(data.name)
  }

  def user = isAuthenticated { implicit request =>
    Ok(request.session("username"))
  }

  def logout = isAuthenticated { implicit request =>
    Ok("loggedout").withNewSession
  }
}
