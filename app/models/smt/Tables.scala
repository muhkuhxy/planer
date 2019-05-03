package models.smt

import java.time.LocalDate

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

case class Assignee(id: Int = -1, name: String, services: List[Int], email: Option[String] = None)

case class AssigneeRow(id: Int, name: String, email: Option[String])
case class Schedule2ServiceRow(assigneeId: Int, serviceId: Int, scheduleId: Int,
  shift: Int)


trait Tables extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  class ServiceTable(tag: Tag) extends Table[Service](tag, "service") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def slots = column[Int]("slots")

    def * = (id, name, slots).mapTo[Service]
  }

  class AssigneeTable(tag: Tag) extends Table[AssigneeRow](tag, "volunteer") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def email = column[Option[String]]("email")

    def * = (id, name, email).mapTo[AssigneeRow]
  }

  class Assignee2ServiceTable(tag: Tag)
    extends Table[(Int, Int)](tag, "volunteer_service") {
    def serviceId = column[Int]("service_id")

    def assigneeId = column[Int]("volunteer_id")

    def assignee = foreignKey("vs_volunteer_fk", assigneeId, assigneeQuery)(_.id)

    def service = foreignKey("vs_service_fk", serviceId, serviceQuery)(_.id)

    def * = (assigneeId, serviceId)
  }

  class UnavailableTable(tag: Tag) extends Table[(Int, Int)](tag, "unavailable") {
    def scheduleId = column[Int]("schedule_id")

    def assigneeId = column[Int]("volunteer_id")

    def * = (scheduleId, assigneeId)
  }

  class Schedule2ServiceTable(tag: Tag)
    extends Table[Schedule2ServiceRow](tag, "schedule_services") {
    def assigneeId = column[Int]("volunteer_id")

    def serviceId = column[Int]("service_id")

    def scheduleId = column[Int]("schedule_id")

    def shift = column[Int]("shift")

    def * = (assigneeId, serviceId, scheduleId, shift).mapTo[Schedule2ServiceRow]
  }

  class PlanTable(tag: Tag) extends Table[PlanRow](tag, "plan") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def * = (id, name).mapTo[PlanRow]
  }

  case class ScheduleRow(id: Int, date: LocalDate, planId: Int)

  class ScheduleTable(tag: Tag) extends Table[ScheduleRow](tag, "schedule") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def date = column[LocalDate]("day")

    def planId = column[Int]("plan_id")

    def * = (id, date, planId).mapTo[ScheduleRow]
  }

  lazy val planQuery = TableQuery[PlanTable]
  lazy val scheduleQuery = TableQuery[ScheduleTable]
  lazy val unavailableQuery = TableQuery[UnavailableTable]
  lazy val serviceQuery = TableQuery[ServiceTable]
  lazy val assigneeQuery = TableQuery[AssigneeTable]
  lazy val assignee2Service = TableQuery[Assignee2ServiceTable]
  lazy val schedule2Service = TableQuery[Schedule2ServiceTable]
}
