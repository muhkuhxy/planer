package models

import play.api.libs.json._

object domainError {
  type JsErrors = Seq[(JsPath, Seq[JsonValidationError])]

  sealed abstract class DomainError
  case class JsonParseError(json: JsValue, errors: JsErrors) extends DomainError
  case object InvalidCredentials extends DomainError
  case object NoPlan extends DomainError
  case object InvalidDateRange extends DomainError
}
