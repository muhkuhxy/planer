package controllers

import javax.inject._
import play.api.mvc._
import play.api.mvc.Security._
import scala.concurrent._

class UserAuthenticatedBuilder (parser: BodyParser[AnyContent])(implicit ec: ExecutionContext)
  extends AuthenticatedBuilder[String]({ req: RequestHeader =>
    req.session.get("username")
  }, parser) {
  @Inject()
  def this(parser: BodyParsers.Default)(implicit ec: ExecutionContext) = {
    this(parser: BodyParser[AnyContent])
  }
}
