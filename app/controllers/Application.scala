package app

import cats._
import cats.data._
import cats.syntax.functor._
import cats.implicits._
import java.time.LocalDate
import models.DomainError._
import play.api.http.Writeable
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Result => PlayResult, Request}
import play.api.mvc.Results._
import play.api.Logger
import scala.language.implicitConversions
import scala.concurrent._

object ErrorDetails {
  def apply(error: Product): ErrorDetails =
    new ErrorDetails(error.productPrefix)

  def apply(error: Product, errors: JsErrors): ErrorDetails =
    new ErrorDetails(error.productPrefix, parseErrors = Some(JsError.toJson(errors)))
}

case class ErrorDetails(`type`: String,
  reason: Option[String] = None,
  parseErrors: Option[JsValue] = None)

object Application {
  val logger = Logger("application")

  def parseBody[T](implicit request: Request[JsValue], reads: Reads[T]): Either[JsonParseError, T] =
    request.body.validate[T].asEither.leftMap(JsonParseError(request.body, _))

  def parseBodyT[T](implicit request: Request[JsValue], reads: Reads[T], f:
  Applicative[Future]): EitherT[Future, JsonParseError, T] =
    EitherT.fromEither[Future](parseBody[T])

  implicit def toHttpResult[F[_] : Functor, E <: DomainError]
    (e: F[Either[E, PlayResult]]): F[PlayResult] =
    Functor[F].map(e)(_.fold(mapErrors, identity))

  implicit def toHttpResult(e: Either[DomainError, PlayResult]): PlayResult =
    toHttpResult[Id, DomainError](e)

  implicit class EitherOps[E, A](val either: Either[E, A]) {
    def eitherT[F[_]: Applicative]: EitherT[F, E, A] = EitherT.fromEither[F](either)
  }

  def right[A](value: Future[A])(implicit f: Functor[Future]):
    EitherT[Future, DomainError, A] =
    EitherT.right[DomainError](value)

  implicit val errorDetailsWrites = Json.writes[ErrorDetails]

  def mapErrors(e: DomainError): PlayResult = e match {
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

  implicit def localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def localDateDescOrdering: Ordering[LocalDate] = localDateOrdering.reverse
}
