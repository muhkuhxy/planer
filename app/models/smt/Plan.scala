package models.smt

import anorm._
import anorm.SqlParser._
import com.google.inject.ImplementedBy
import java.sql.Connection
import java.time._
import java.time.format._
import java.time.temporal._
import javax.inject._
import java.util.Date
import play.api.Play.current
import play.api.db._
import play.api.Logger
import scala.language.postfixOps
import scala.language.implicitConversions
import scala.collection.mutable.ListBuffer

case class Plan(id: Int, name: String, parts: List[Schedule], services: List[Service])
case class Schedule(id: Int, date: LocalDate, unavailable: List[Int], assignments: List[Assignment])
case class Assignment(s: Int, shifts: List[Option[Int]])
case class Service(id: Int, name: String, slots: Int)

case class PlanShell(id: Int, name: String)

case class PlanUpdateRequest(id: Int, parts: List[PartsRequest])
case class PartsRequest(id: Int, date: LocalDate, unavailable: List[Int], assignments: List[Assignment])

@ImplementedBy(classOf[DefaultPlanRepository])
trait PlanRepository {
  def save(plan: PlanUpdateRequest)
  def list: List[PlanShell]
  def find(id: Long): Option[Plan]
  def remove(id: Long)
  def create(from: LocalDate, to: LocalDate): Long
}

class DefaultPlanRepository @Inject()(db: Database) extends PlanRepository with Helper {
  val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  def plans(implicit c: Connection) = SQL"select id, name from plan order by id desc".as(int("id") ~ str("name") map flatten *)

  def planById(id: Long)(implicit c: Connection) =
    SQL"select id, name from plan where id = $id".as(int("id") ~ str("name") map flatten singleOpt)

  def schedules(id: Long)(implicit c: Connection): Iterable[RawSchedule] =
    SQL"""
      select s.id, day, volunteer_id, service_id, shift, slots from schedule s
      join schedule_services ss on s.id = ss.schedule_id
      join service on service.id = ss.service_id
      join volunteer v on v.id = volunteer_id
      where plan_id = $id and v.active
      order by day, service_id, shift
    """.as(Macro.indexedParser[RawSchedule].*)

  def groupWith[A, B](xs: Iterable[A])(f: A => B): Iterable[Iterable[A]] =
    xs.groupBy(f).map(_._2).toIterable

  case class RawSchedule(id: Int, day: Date, volunteerId: Int, serviceId: Int, shift: Int, slots: Int)

  def scheduleGroups(id: Long)(implicit c: Connection): Iterable[Iterable[RawSchedule]] =
    groupWith(schedules(id))(_.id)

  def unavailableOn(scheduleId: Int)(implicit c: Connection) =
    SQL"""
      select volunteer_id id from unavailable u
      join volunteer v on v.id = u.volunteer_id
      where schedule_id = $scheduleId and v.active
    """.as(int(1) *)

  def find(id: Long) = db.withConnection { implicit c =>
    planById(id) map { case (planId, planName) =>
      Plan(planId, planName, scheduleGroups(planId) map { case schedule =>
        val (scheduleId, day) = schedule.headOption.map(x => (x.id, x.day)).get
        val assignments = groupWith(schedule)(_.serviceId) map { case serviceGroup =>
          val (service, slots) = serviceGroup.headOption.map(x => (x.serviceId, x.slots)).get
          val assignees = ListBuffer.fill(slots)(None: Option[Int])
          serviceGroup.map { case RawSchedule(_, _, volunteer, _, shift, _) =>
            assignees(shift) = Some(volunteer)
          }
          Assignment(service, assignees.toList)
        }
        Schedule(scheduleId, day, unavailableOn(scheduleId), assignments.toList)
      } toList, services.toList)
    }
  }

  def list = db.withConnection { implicit c =>
    for {
      (id, name) <- SQL"select id, name from plan order by id desc".as(int("id") ~ str("name") map flatten *)
    } yield PlanShell(id, name)
  }

  def save(plan: PlanUpdateRequest) = db.withConnection { implicit c =>
    plan.parts.foreach { part =>
      SQL("update schedule set day = {date} where id = {id}")
        .on('date -> toDate(part.date), 'id -> part.id)
        .executeUpdate()
      val deleted = deleteScheduleRefs(part.id)
      Logger.debug(s"deleted (unavailable, assignments): $deleted")
      part.unavailable.foreach { whoId =>
        SQL"insert into unavailable values (${part.id}, $whoId)".executeInsert()
      }
      part.assignments.foreach { case Assignment(serviceId, shifts) =>
        shifts.zipWithIndex foreach {
          case (Some(volunteerId), shift) =>
            SQL"insert into schedule_services values ($volunteerId, $serviceId, ${part.id}, $shift)"
              .executeInsert()
          case _ =>
        }
      }
    }
  }

  def remove(id: Long) = db.withConnection { implicit c =>
    val scheduleIds = SQL"select id from schedule where plan_id = $id".as(multiIds)
    scheduleIds.foreach(deleteScheduleRefs(_))
    val count = SQL"delete from schedule where id in (${scheduleIds})".executeUpdate()
    Logger.info(s"$count schedules deleted")
    SQL"delete from plan where id = $id".executeUpdate
  }

  def create(from: LocalDate, to: LocalDate) = db.withConnection { implicit c =>
    Logger.debug(s"formatted to ${to.format(dateFormat)}")
    val planId = SQL("insert into plan(name) values ({name})")
      .on('name -> s"${from.format(dateFormat)} bis ${to.format(dateFormat)}")
      .executeInsert().get
    val dates = daysBetween(from, to, Seq(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))
    dates.foreach { date =>
      val dbDate = toDate(date)
      SQL"insert into schedule(plan_id, day) values ($planId, $dbDate)".executeInsert()
    }
    planId
  }

  def daysBetween(from: LocalDate, to: LocalDate, weekDays: Seq[DayOfWeek]): Stream[LocalDate] = {
    lazy val loopingWeekdays: Stream[DayOfWeek] = weekDays.toStream #::: loopingWeekdays
    // drop first day of week if from is the same DOW, otherwise we skip e.g. from Friday to Friday
    // from will be included anyway b/c of scanLeft
    val fixedWeekdays = loopingWeekdays.dropWhile(dow => dow == from.getDayOfWeek)
    val nextWeekday = fixedWeekdays map {
      TemporalAdjusters.next
    }

    nextWeekday.scanLeft(from)((date, adjuster) => {
      date.`with`(adjuster)
    }).dropWhile(d => !weekDays.contains(d.getDayOfWeek))
      .takeWhile(d => d.isBefore(to) || d.isEqual(to))
  }

  def toDate(local: LocalDate): java.util.Date =
    java.util.Date.from(local.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant)

  implicit def toLocalDate(date: Date): LocalDate =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()).toLocalDate()

  def deleteScheduleRefs(scheduleId: Long)(implicit c: Connection) = {
    val unavailable = SQL"delete from unavailable where schedule_id = $scheduleId".executeUpdate()
    val assignments = SQL"delete from schedule_services where schedule_id = $scheduleId".executeUpdate()
    (unavailable, assignments)
  }
}
