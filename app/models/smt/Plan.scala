package models.smt

import anorm._
import anorm.SqlParser._
import anorm.Macro.ColumnNaming
import cats.data.NonEmptyList
import cats.implicits._
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

  def list = db.withConnection { implicit c =>
    for {
      (id, name) <- SQL"select id, name from plan order by id desc".as(int("id") ~ str("name") map flatten *)
    } yield PlanShell(id, name)
  }

  def find(id: Long) = db.withConnection { implicit c =>
    for {
      (planId, planName) <- planById(id)
    } yield {
      Plan(planId, planName, schedules(planId).map(makeSchedule).toList, services.toList)
    }
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
    if (scheduleIds.nonEmpty) {
      val count = SQL"delete from schedule where id in (${scheduleIds})".executeUpdate()
      Logger.info(s"$count schedules deleted")
    }
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

  def makeSchedule(schedule: MeetingDay)(implicit c: Connection): Schedule = {
    val assignments = groupWith(schedule.assignments)(_.serviceId).map(makeAssignment)
    Schedule(schedule.id, schedule.day, unavailableOn(schedule.id), assignments.toList)
  }

  def makeAssignment(serviceGroup: NonEmptyList[AssignmentExtra]): Assignment = {
    val (service, slots) = (serviceGroup.head.serviceId, serviceGroup.head.slots)
    val shifts = ListBuffer.fill(slots)(None: Option[Int])
    serviceGroup.map { ae =>
      shifts(ae.shift) = Some(ae.volunteerId)
    }
    Assignment(service, shifts.toList)
  }

  val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  def plans(implicit c: Connection) =
    SQL"select id, name from plan order by id desc"
      .as(int("id") ~ str("name") map flatten *)

  def planById(id: Long)(implicit c: Connection) =
    SQL"select id, name from plan where id = $id"
      .as(int("id") ~ str("name") map flatten singleOpt)

  def schedules(id: Long)(implicit c: Connection): List[MeetingDay] =
    SQL"""
      select s.id, day, volunteer_id, service_id, shift, slots from schedule s
      left join schedule_services ss on s.id = ss.schedule_id
      left join service on service.id = ss.service_id
      left join volunteer v on v.id = volunteer_id
      where plan_id = $id
    """.as(parser *)
    .groupBy(_._1)
    .mapValues(_.map(_._2).flatten)
    .map { case (md, as) => md.copy(assignments = as) }
    .toList
    .sortBy(_.day)

  case class MeetingDay(id: Int, day: LocalDate, assignments: List[AssignmentExtra])
  case class AssignmentExtra(volunteerId: Int, serviceId: Int, shift: Int, slots: Int)

  val simpleMeetingDayParser: RowParser[MeetingDay] = {
    int("id") ~ get[Date]("day") map {
      case id ~ day => MeetingDay(id, day, Nil)
    }
  }

  val assignmentExtraParser = Macro.namedParser[AssignmentExtra](ColumnNaming.SnakeCase)

  val parser: RowParser[(MeetingDay, Option[AssignmentExtra])] = {
    simpleMeetingDayParser ~ assignmentExtraParser.? map flatten
  }

  def groupWith[A, B](xs: Iterable[A])(f: A => B): Iterable[NonEmptyList[A]] =
    xs.groupBy(f).map(ys => NonEmptyList.fromListUnsafe(ys._2.toList))

  def unavailableOn(scheduleId: Int)(implicit c: Connection) =
    SQL"""
      select volunteer_id id from unavailable u
      join volunteer v on v.id = u.volunteer_id
      where schedule_id = $scheduleId
    """.as(int(1) *)

  def daysBetween(from: LocalDate, to: LocalDate, weekDays: Seq[DayOfWeek]): Stream[LocalDate] = {
    lazy val loopingWeekdays: Stream[DayOfWeek] = weekDays.toStream #::: loopingWeekdays
    // drop first day of week if from is the same DOW, otherwise we skip e.g. from Friday to Friday
    // from will be included anyway b/c of scanLeft
    val fixedWeekdays = loopingWeekdays.dropWhile(dow => dow == from.getDayOfWeek)
    val weekdayAdjusters = fixedWeekdays.map(TemporalAdjusters.next)

    weekdayAdjusters.scanLeft(from)((date, nextWeekday) => {
      date `with` nextWeekday
    }).dropWhile(d => !weekDays.contains(d.getDayOfWeek))
      .takeWhile(d => d.isBefore(to) || d.isEqual(to))
  }

  def toDate(local: LocalDate): java.util.Date =
    Date.from(local.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant)

  implicit def toLocalDate(date: Date): LocalDate =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()).toLocalDate()

  def deleteScheduleRefs(scheduleId: Long)(implicit c: Connection) = {
    val unavailable = SQL"delete from unavailable where schedule_id = $scheduleId".executeUpdate()
    val assignments = SQL"delete from schedule_services where schedule_id = $scheduleId".executeUpdate()
    (unavailable, assignments)
  }
}
