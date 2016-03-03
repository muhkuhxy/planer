package models

import anorm._
import anorm.SqlParser._
import java.sql.Connection
import java.time._
import java.time.format._
import java.time.temporal._
import java.util.Date
import play.api.Play.current
import play.api.db.DB
import play.api.Logger
import scala.language.postfixOps
import scala.language.implicitConversions

trait Helper {
  def indexByFirst[A](values: List[(A, A)]): Map[A, List[A]] =
    values.groupBy(_._1).map({ case(k,v) =>
      k -> v.map(_._2)
    })

  val singleId = int("id").single
  val multiIds = int("id") *
  val idAndName = int("id") ~ str("name") map flatten *
  def volunteers(implicit c: Connection) = SQL("select id, name from volunteer").as(idAndName)
  def volunteersByName(implicit c: Connection) = volunteers.map(v => v._2 -> v._1).toMap
  def services(implicit c: Connection) = SQL("select id, name from service").as(idAndName)
  def servicesByName(implicit c: Connection) = services.map(s => s._2 -> s._1).toMap
}

object DefaultAssigneeRepository extends AssigneeRepository with Helper {

  def getAssignees = DB.withConnection { implicit c =>
    val result = SQL"""
      select v.name as volunteer, s.name as service from volunteer v
      join volunteer_service vs on v.id = vs.volunteer_id
      join service s on s.id = vs.service_id
    """.as(str("volunteer") ~ str("service") map (flatten) *)
    val assignees = for {
      (name, services) <- indexByFirst(result)
    } yield Assignee(name, services.toSet)
    assignees.toList.sortBy(_.name)
  }

  def save(helpers: List[Assignee]) = DB.withConnection { implicit c =>
    val serviceIds = servicesByName
    val volunteerIds = volunteersByName
    sealed trait AssigneeOp {
      def execute()
    }
    case class UpdateServices(as: Assignee) extends AssigneeOp {
      val asId = volunteerIds(as.name)
      def execute {
        Logger.info(s"updating services $as")
        SQL"delete from volunteer_service where volunteer_id = $asId".executeUpdate()
        for(service <- as.services)
          SQL"insert into volunteer_service values ($asId, ${serviceIds(service)})".executeInsert()
      }
    }
    case class Add(as: Assignee) extends AssigneeOp {
      def execute {
        Logger.info(s"adding $as")
        val asId = SQL"insert into volunteer(name) values (${as.name})".executeInsert()
        for(service <- as.services)
          SQL"insert into volunteer_service values ($asId, ${serviceIds(service)})".executeInsert()
      }
    }
    def calculateDifferences(old: List[Assignee], now: List[Assignee]): Seq[AssigneeOp] = {
      import scala.collection.mutable.ListBuffer
      val byName = old.map( as => as.name -> as ).toMap
      var ops = ListBuffer.empty[AssigneeOp]
      now.foreach { case as @ Assignee(name, services) =>
        if(byName.contains(name)) {
          if(byName(name).services != services) {
            ops += UpdateServices(as)
          }
        } else {
          ops += Add(as)
        }
      }
      ops
    }
    val old = getAssignees
    calculateDifferences(old, helpers) foreach (_.execute())
  }

}

object DefaultPlanRepository extends PlanRepository with Helper {
  val dateFormat = DateTimeFormatter.ofPattern("dd.MM.YYYY")

  def find(id: Long) = DB.withConnection { implicit c =>
    val plan = SQL"select id, name from plan where id = $id".as(int("id") ~ str("name") map flatten single)
    val schedulesResult = SQL"select id, day from schedule where plan_id = $id"
      .as(int("id") ~ get[Date]("day") map flatten *)
    val schedules = schedulesResult map { case (scheduleId, when) =>
      val unavailable = SQL"""
        select v.name from unavailable u
        join volunteer v on v.id = u.volunteer_id
        where schedule_id = $scheduleId
      """.as(str(1) *)
      val assigned = SQL"""
        select s.name service, v.name volunteer from schedule_services ss
        join service s on s.id = ss.service_id
        join volunteer v on v.id = ss.volunteer_id
        where schedule_id = $scheduleId
      """.as(str("service") ~ str("volunteer") map flatten *)
      val assignments = indexByFirst(assigned)
      Schedule(when, unavailable, assignments)
    }
    Logger.debug(s"$schedules")
    Plan(plan._1, plan._2, schedules)
  }

  def list: List[Plan] = DB.withConnection { implicit c =>
    SQL"select id, name from plan order by id desc".as(int("id") ~ str("name") map flatten *) map { n =>
      Plan(n._1, n._2, List())
    }
  }

  def save(plan: Plan) = DB.withConnection { implicit c =>
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
        assignment._2 filter( _.nonEmpty ) foreach { name =>
          val volunteerId = volunteerIds(name)
          SQL"insert into schedule_services values ($volunteerId, $serviceId, $scheduleId)"
            .executeInsert()
        }
      }
    }
  }

  def remove(id: Long) = DB.withConnection { implicit c =>
    val scheduleIds = SQL"select id from schedule where plan_id = $id".as(multiIds)
    scheduleIds.foreach(deleteScheduleRefs(_))
    val count = SQL"delete from schedule where id in (${scheduleIds})".executeUpdate()
    Logger.info(s"$count schedules deleted")
    SQL"delete from plan where id = $id".executeUpdate
  }

  def create(from: LocalDate, to: LocalDate) = DB.withConnection { implicit c =>
    val planId = SQL("insert into plan(name) values ({name})")
      .on('name -> s"Plan vom ${from.format(dateFormat)} bis ${to.format(dateFormat)}")
      .executeInsert().get
    val dates = daysBetween(from, to.plusDays(7), Seq(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))
    dates.foreach { date =>
      val dbDate = toDate(date)
      SQL"insert into schedule(plan_id, day) values ($planId, $dbDate)".executeInsert()
    }
    planId
  }

  def daysBetween(from: LocalDate, to: LocalDate, weekdays: Seq[DayOfWeek]) = {
    val adjusters = weekdays map { TemporalAdjusters.next(_) }
    val infIt = new Iterator[TemporalAdjuster] {
      val hasNext = true
      var count = -1
      def next = {
        count = (count + 1) % adjusters.size
        adjusters(count)
      }
    }
    var date = from
    val dates = infIt.map(adj => {
      date = date.`with`(adj)
      date
    }).takeWhile(_.isBefore(to))
    dates
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
