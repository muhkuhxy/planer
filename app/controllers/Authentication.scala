package controllers

import anorm._
import anorm.SqlParser._
import play.api.mvc._
import play.api.Logger
import play.api.i18n._
import play.api.mvc.Results._
import play.api.Play.current
import play.api.mvc.Security._
import play.api.data._
import play.api.data.Forms._
import play.api.db.Database
import play.api.libs.json._
import javax.inject._
import org.mindrot.jbcrypt.BCrypt

trait Security {
  def getUserFromRequest(req: RequestHeader): Option[String] = req.session.get("username")
  def onUnauthorized(req: RequestHeader) = Unauthorized
  object Authenticated extends AuthenticatedBuilder(getUserFromRequest, onUnauthorized)
}

object AuthenticationController {
  case class LoginForm(name: String, password: String)
  val loginForm = Form(
    mapping(
      "name"->text,
      "password"->text
    )(LoginForm.apply)(LoginForm.unapply)
  )

}

class AuthenticationController @Inject()(db: Database) (val messagesApi: MessagesApi) extends Controller with Security with I18nSupport {
  import AuthenticationController._

  def loginPage = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def checkCredentials(user: String, password: String): Option[String] = db.withConnection { implicit c =>
    val maybePasswd = SQL"select password from appuser where username = $user"
      .as(scalar[String].singleOpt)
    for {
      hashed <- maybePasswd if BCrypt.checkpw(password, hashed)
    } yield user
  }

  def login = Action(BodyParsers.parse.form(loginForm)) { implicit request =>
    val loginData = request.body
    checkCredentials(loginData.name, loginData.password) match {
      case Some(user) =>
        Redirect(smt.routes.PlanController.overview)
          .withSession(request.session + ("username" -> loginData.name))
      case None =>
        Redirect(routes.AuthenticationController.loginPage)
    }
  }

  case class LoginData(name: String, password: String)
  implicit val loginReads = Json.reads[LoginData]

  def spaLogin = Action(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[LoginData]
    result.fold(
      errors => {
        val readableErrors = JsError.toJson(errors)
        Logger.error(s"login error: $errors")
        BadRequest(readableErrors)
      },
      data => {
        checkCredentials(data.name, data.password) match {
          case Some(user) =>
            Ok(data.name).withSession(request.session + ("username" -> data.name))
          case None =>
            Unauthorized
        }
      }
    )
  }


  def logout = Authenticated { implicit request =>
    Ok("loggedout").withSession(request.session - "username")
  }
}
