package models.smt

import app.Application.logger
import cats.implicits._
import scala.concurrent._

import play.api.db.slick._
import slick.jdbc.JdbcProfile
import slick.basic._

case class Assignee(id: Int = -1, name: String, services: List[Int], email: Option[String] = None)

trait SlickAssigneeDb extends HasDatabaseConfigProvider[JdbcProfile] {
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

  class Assignee2ServiceTable(tag: Tag) extends Table[(Int, Int)](tag, "volunteer_service") {
    def serviceId = column[Int]("service_id")
    def assigneeId = column[Int]("volunteer_id")

    def assignee = foreignKey("vs_volunteer_fk", assigneeId, assigneeQuery)(_.id)
    def service = foreignKey("vs_service_fk", serviceId, serviceQuery)(_.id)

    def * = (serviceId, assigneeId)
  }

  class UnavailableTable(tag: Tag) extends Table[(Int, Int)](tag, "unavailable") {
    def scheduleId = column[Int]("schedule_id")
    def assigneeId = column[Int]("volunteer_id")

    def * = (scheduleId, assigneeId)
  }

  class Schedule2ServiceTable(tag: Tag) extends Table[Schedule2ServiceRow](tag, "schedule_services") {
    def assigneeId = column[Int]("volunteer_id")
    def serviceId = column[Int]("service_id")
    def scheduleId = column[Int]("schedule_id")
    def shift = column[Int]("shift")

    def * = (assigneeId, serviceId, scheduleId, shift).mapTo[Schedule2ServiceRow]
  }

  lazy val unavailableQuery = TableQuery[UnavailableTable]
  lazy val serviceQuery = TableQuery[ServiceTable]
  lazy val assigneeQuery = TableQuery[AssigneeTable]
  lazy val assignee2Service = TableQuery[Assignee2ServiceTable]
  lazy val schedule2Service = TableQuery[Schedule2ServiceTable]

  def getServices: Future[Seq[Service]] =
    db.run(serviceQuery.result)

  def getAssignees(implicit ec: ExecutionContext): Future[List[Assignee]] =
    db.run(flatAssignees.result.map {
      _.groupBy { case (assignee, service) => assignee }
        .map { case (a, group) => {
          val services = group.map { case (_, service) => service.map(_.id) } collect { case Some(x) => x }
          Assignee(a.id, a.name, services.toList, a.email)
        }}.toList.sortBy(_.name)
    })

  case class AssigneeRow(id: Int, name: String, email: Option[String])
  case class Schedule2ServiceRow(assigneeId: Int, serviceId: Int, scheduleId: Int,
    shift: Int)

  lazy val flatAssignees =
    assigneeQuery
      .joinLeft(assignee2Service).on { case (a, a2s) => a.id === a2s.assigneeId }
      .joinLeft(serviceQuery).on { case ((a, a2s), s) => a2s.map(_.serviceId) === s.id }
      .map { case ((assignee, _), service) => (assignee, service) }

  def updateAssignee(a: Assignee): DBIO[Unit] = DBIO.seq(
    assigneeQuery.filter(_.id === a.id)
        .map(x => (x.name, x.email))
        .update((a.name, a.email)),
    removeServicesFor(a.id),
    addServicesFor(a.id, a.services)
  ).transactionally

  def addAssignee(a: Assignee)(implicit ec: ExecutionContext): DBIO[Unit] = (
    for {
      id <- assigneeQuery returning assigneeQuery.map(_.id) += AssigneeRow(0, a.name, a.email)
      _ <- addServicesFor(id, a.services)
    } yield ()
  ).transactionally

  def removeServicesFor(assigneeId: Int) =
    assignee2Service.filter(_.assigneeId === assigneeId).delete

  def addServicesFor(assigneeId: Int, services: List[Int]) =
    assignee2Service ++= services.map((assigneeId, _))

  def removeAssignee(a: Assignee): DBIO[Unit] = DBIO.seq(
    removeServicesFor(a.id),
    schedule2Service.filter(_.assigneeId === a.id).delete,
    unavailableQuery.filter(_.assigneeId === a.id).delete,
    assigneeQuery.filter(_.id === a.id).delete
  ).transactionally

  def saveAssignees(previous: Seq[Assignee], current: List[Assignee])
      (implicit ec: ExecutionContext): Future[List[Unit]] = {
    sealed trait AssigneeOp
    case class Update(as: Assignee) extends AssigneeOp
    case class Add(as: Assignee) extends AssigneeOp
    case class Remove(as: Assignee) extends AssigneeOp

    def calculateDifferences(previous: Seq[Assignee], current: List[Assignee]): List[AssigneeOp] = {
      import scala.collection.mutable.ListBuffer
      val prevById = previous.map( as => as.id -> as ).toMap
      var ops = ListBuffer.empty[AssigneeOp]
      current.foreach { case as @ Assignee(id, name, services, email) =>
        if(id > 0) {
          val existing = prevById(id)
          if(existing != as) {
            ops += Update(as)
          }
        } else {
          ops += Add(as)
        }
      }
      val previousIds = previous.map(_.id).toSet
      val currentIds = current.map(_.id).toSet
      val removeIds = previousIds.diff(currentIds).filter(_ > 0)
      ops ++= removeIds.map(prevById).map(Remove)
      ops.toList
    }

    def execute(op: AssigneeOp): DBIO[Unit] =
      op match {
        case Add(a@Assignee(id, name, services, email)) => {
          logger.info(s"adding $a")
          addAssignee(a)
        }
        case Update(a@Assignee(id, name, services, email)) => {
          require(id > 0)
          logger.info(s"updating services $a with id $id")
          updateAssignee(a)
        }
        case Remove(a) => {
          logger.info(s"removing $a")
          removeAssignee(a)
        }
      }

    val actionPlan = calculateDifferences(previous, current)
    val actions = actionPlan.map(execute)
    db.run(DBIO.sequence(actions))
  }
}
