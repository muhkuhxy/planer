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
import play.api.db.DB
import javax.inject._
import org.mindrot.jbcrypt.BCrypt

trait Security {
  def getUserFromRequest(req: RequestHeader): Option[String] = req.session.get("username")
  object Authenticated extends AuthenticatedBuilder(req => getUserFromRequest(req))
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

class AuthenticationController @Inject() (val messagesApi: MessagesApi) extends Controller with Security with I18nSupport {
  import AuthenticationController._

  def loginPage = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def checkCredentials(user: String, password: String): Option[String] = DB.withConnection { implicit c =>
    val maybePasswd = SQL"select password from user where username = $user"
      .as(scalar[String].singleOpt)
    for {
      hashed <- maybePasswd if BCrypt.checkpw(password, hashed)
    } yield user
  }

  def login = Action(BodyParsers.parse.form(loginForm)) { implicit request =>
    val loginData = request.body
    checkCredentials(loginData.name, loginData.password) match {
      case Some(user) =>
        Redirect(routes.PlanerController.overview)
          .withSession(request.session + ("username" -> loginData.name))
      case None =>
        Redirect(routes.AuthenticationController.loginPage)
    }
  }

  def logout = Authenticated { implicit request =>
    Ok("loggedout").withSession(request.session - "username")
  }
}
