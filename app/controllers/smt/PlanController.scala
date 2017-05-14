package controllers.smt

import controllers.Security
import java.time.LocalDate
import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.smt._

class PlanController @Inject()(assigness: AssigneeRepository, plans: PlanRepository) extends Controller with Security {

  case class Range(from: LocalDate, to: LocalDate)
  implicit val ldRead = new Reads[LocalDate] {
    def reads(json: JsValue) = JsSuccess(LocalDate.parse(json.as[String]))
  }
  implicit val SchedRead = Json.reads[Schedule]
  implicit val PlanRead = Json.reads[Plan]
  implicit val RangeRead = Json.reads[Range]

  def overview = Authenticated {
    Ok(views.html.smt.overview(plans.list))
  }

  def show(id: Long) = Authenticated {
    Ok(views.html.smt.plan(assigness.getAssignees, plans.find(id)))
  }

  def create = Authenticated(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[Range]
    result.fold(
      errors => {
        val readableErrors = JsError.toJson(errors)
        Logger.error(s"couldnt create plan: $errors")
        BadRequest(readableErrors)
      },
      range => {
        val id = plans.create(range.from, range.to)
        Ok(routes.PlanController.show(id).absoluteURL)
      }
    )
  }

  def save(id: Long) = Authenticated(BodyParsers.parse.json) { request =>
    val plan = parseBody(request.body)
    plans.save(plan)
    Ok("saved")
  }

  def parseBody(body: JsValue): Plan = {
    val id = body(0).as[Int]
    val name = body(1).as[String]
    val parts = body(2).as[JsArray]
    val names = parts(0).as[Array[String]]
    val services = Array("sicherheit", "mikro", "tonanlage")
    def lookupName(id: JsValue): Option[String] = id match {
      case JsNumber(i) if i != -1 => Some(names(i.intValue))
      case _ => None
    }
    val schedules = parts(1).as[JsArray].value.map({
        case JsArray(Seq(date, JsArray(assignments), JsArray(unavailable))) =>
        Schedule(date.as[LocalDate],
          unavailable.map(lookupName(_)).toList.flatten,
          assignments.zipWithIndex.map({
              case (serviceAssignment: JsArray, serviceIndex) => {
                val s: String = services(serviceIndex)
                val assgs = serviceAssignment.value.map(lookupName(_)).toList.flatten
                s -> assgs
              }
          }).toMap)
      }).toList
    Plan(id, name, schedules)
  }

  def remove(id: Long) = Authenticated {
    plans.remove(id)
    Ok("deleted")
  }

}
