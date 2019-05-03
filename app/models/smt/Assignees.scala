package models.smt

import scala.concurrent._

trait SlickAssigneeDb extends Tables {
  import profile.api._

  lazy val flatAssignees =
    assigneeQuery
      .joinLeft(assignee2Service).on { case (a, a2s) => a.id === a2s.assigneeId }
      .joinLeft(serviceQuery).on { case ((a, a2s), s) => a2s.map(_.serviceId) === s.id }
      .map { case ((assignee, _), service) => (assignee, service) }

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

    def calculateDifferences(previous: Seq[Assignee], current: Seq[Assignee]):
    List[AssigneeOp] = {
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
      ops ++= removeIds.map(prevById andThen Remove)
      ops.toList
    }

    def execute(op: AssigneeOp): DBIO[Unit] =
      op match {
        case Add(assignee) => addAssignee(assignee)
        case Remove(assignee) => removeAssignee(assignee)
        case Update(assignee) => {
          require(assignee.id > 0)
          updateAssignee(assignee)
        }
      }

    val actionPlan = calculateDifferences(previous, current)
    val actions = actionPlan.map(execute)
    db.run(DBIO.sequence(actions).transactionally)
  }
}
