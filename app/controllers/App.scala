package app

import cats.implicits._
import play.api.http.Writeable
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Result => PlayResult, Request}
import play.api.mvc.Results._
import play.api.Logger
import scala.language.implicitConversions

object Application {
  val logger = Logger("application")

  sealed abstract class DomainError
  case class JsonParseError(json: JsValue, errors: JsErrors) extends DomainError
  case object InvalidCredentials extends DomainError
  case object NoPlan extends DomainError

  type JsErrors = Seq[(JsPath, Seq[JsonValidationError])]

  def parseBody[T](implicit request: Request[JsValue], reads: Reads[T]): Either[JsonParseError, T] =
    request.body.validate[T].asEither.leftMap(JsonParseError(request.body, _))

  implicit def toHttpResult(e: Either[DomainError, PlayResult]): PlayResult =
    e.fold(mapErrors, identity)

  def mapErrors(e: DomainError): PlayResult = e match {
    case JsonParseError(json, errors) => {
      logger.error(s"invalid json in $json")
      logger.error(s"errors: $errors")
      BadRequest
    }
    case InvalidCredentials => Unauthorized
    case NoPlan => NotFound
  }
}


