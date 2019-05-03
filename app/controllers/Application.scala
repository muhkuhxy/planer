package app

import java.time.LocalDate

import cats._
import cats.data._
import cats.implicits._
import models.domainError._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Request, Result}
import play.api.mvc.Results._

import scala.concurrent._
import scala.language.implicitConversions

object ErrorDetails {
  def apply(error: Product): ErrorDetails =
    new ErrorDetails(error.productPrefix)

  def apply(error: Product, errors: JsErrors): ErrorDetails =
    new ErrorDetails(error.productPrefix, parseErrors = Some(JsError.toJson(errors)))
}

case class ErrorDetails(
  `type`: String,
  reason: Option[String] = None,
  parseErrors: Option[JsValue] = None)

object parsers {
  def parseBody[T](implicit request: Request[JsValue], reads: Reads[T]): Either[JsonParseError, T] =
    request.body.validate[T].asEither.leftMap(JsonParseError(request.body, _))

  def parseBodyT[T](implicit request: Request[JsValue], reads: Reads[T], f:
  Applicative[Future]): EitherT[Future, JsonParseError, T] =
    EitherT.fromEither[Future](parseBody[T])
}

object either {
  implicit class EitherOps[E, A](val either: Either[E, A]) {
    def eitherT[F[_]: Applicative]: EitherT[F, E, A] = EitherT.fromEither[F](either)
  }

  def right[A](value: Future[A])(implicit f: Functor[Future]):
  EitherT[Future, DomainError, A] =
    EitherT.right[DomainError](value)
}

object Application {
  val logger = Logger("application")

  implicit def localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def localDateDescOrdering: Ordering[LocalDate] = localDateOrdering.reverse
}

object results {
  import Application.logger

  implicit def toHttpResult[F[_] : Functor, E <: DomainError]
  (e: F[Either[E, Result]]): F[Result] =
    Functor[F].map(e)(_.fold(mapErrors, identity))

  implicit def toHttpResult(e: Either[DomainError, Result]): Result =
    toHttpResult[Id, DomainError](e)

  implicit val errorDetailsWrites = Json.writes[ErrorDetails]

  def mapErrors(e: DomainError): Result = e match {
    case e @ JsonParseError(json, errors) => {
      logger.error(s"invalid json in $json")
      logger.error(s"errors: $errors")
      BadRequest {
        Json.toJson(ErrorDetails(e, errors))
      }
    }
    case InvalidCredentials => Unauthorized
    case NoPlan => NotFound
    case e @ InvalidDateRange => BadRequest {
      Json.toJson(ErrorDetails(e))
    }
  }
}

