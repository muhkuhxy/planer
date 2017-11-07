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

case class Plan(id: Int, name: String, parts: List[Schedule])
case class Schedule(date: LocalDate, unavailable: List[String], assignments: Map[String,List[String]])

@ImplementedBy(classOf[DefaultPlanRepository])
trait PlanRepository {
  def save(plan: Plan)
  def list: List[Plan]
  def find(id: Long): Plan
  def remove(id: Long)
  def create(from: LocalDate, to: LocalDate): Long
}

class DefaultPlanRepository @Inject()(db: Database) extends PlanRepository with Helper {
  val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  def find(id: Long) = db.withConnection { implicit c =>
    val plan = SQL"select id, name from plan where id = $id".as(int("id") ~ str("name") map flatten single)
    val schedulesResult = SQL"select id, day from schedule where plan_id = $id order by day"
      .as(int("id") ~ get[Date]("day") map flatten *)
    val schedules = schedulesResult map { case (scheduleId, when) =>
      val unavailable = SQL"""
        select v.name from unavailable u
        join volunteer v on v.id = u.volunteer_id
        where schedule_id = $scheduleId and v.active
      """.as(str(1) *)
      val assigned = SQL"""
        select s.name service, v.name volunteer from schedule_services ss
        join service s on s.id = ss.service_id
        join volunteer v on v.id = ss.volunteer_id
        where schedule_id = $scheduleId and v.active
        order by ss.shift
      """.as(str("service") ~ str("volunteer") map flatten *)
      val assignments = indexByFirst(assigned)
      Schedule(when, unavailable, assignments)
    }
    Plan(plan._1, plan._2, schedules)
  }

  def list: List[Plan] = db.withConnection { implicit c =>
    SQL"select id, name from plan order by id desc".as(int("id") ~ str("name") map flatten *) map { n =>
      Plan(n._1, n._2, List())
    }
  }

  def save(plan: Plan) = db.withConnection { implicit c =>
    val volunteerIds = volunteersByName
    val serviceIds = servicesByName
    plan.parts.foreach { part =>
      val scheduleId = SQL("select id from schedule where day = {date} and plan_id = {plan}")
        .on('date -> toDate(part.date), 'plan -> plan.id)
        .as(int("id").single)
      val deleted = deleteScheduleRefs(scheduleId)
      Logger.debug(s"deleted (unavailable, assignments): $deleted")
      part.unavailable.foreach { who =>
        val whoId = volunteerIds(who)
        SQL"insert into unavailable values ($scheduleId, $whoId)".executeInsert()
      }
      part.assignments.foreach { assignment =>
        val serviceId = serviceIds(assignment._1)
        assignment._2.zipWithIndex.filter( _._1.nonEmpty ) foreach { case (name, shift) =>
          val volunteerId = volunteerIds(name)
          SQL"insert into schedule_services values ($volunteerId, $serviceId, $scheduleId, $shift)"
            .executeInsert()
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
      .on('name -> s"Plan vom ${from.format(dateFormat)} bis ${to.format(dateFormat)}")
      .executeInsert().get
    val dates = daysBetween(from, to, Seq(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))
    dates.foreach { date =>
      val dbDate = toDate(date)
      SQL"insert into schedule(plan_id, day) values ($planId, $dbDate)".executeInsert()
    }
    planId
  }

  def daysBetween(from: LocalDate, to: LocalDate, weekDays: Seq[DayOfWeek]): Stream[LocalDate] = {
    val adjusters = weekDays map {
      TemporalAdjusters.next
    }
    lazy val loopingAdjusters: Stream[TemporalAdjuster] = adjusters.toStream #::: loopingAdjusters

    loopingAdjusters.scanLeft(from)((date, adjuster) => {
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
