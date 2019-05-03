package models.smt

import java.time._
import java.time.format._
import java.time.temporal._

import app.Application._
import cats.data._
import models._

import scala.concurrent._
import scala.language.{implicitConversions, postfixOps}

case class Plan(id: Int, name: String, parts: List[Schedule], services: List[Service])
case class Schedule(id: Int, date: LocalDate, unavailable: List[Int], assignments: List[Assignment])
case class Assignment(s: Int, shifts: List[Option[Int]])
case class Service(id: Int, name: String, slots: Int)

case class PlanRow(id: Int, name: String)

case class PlanUpdateRequest(id: Int, parts: List[PartsRequest])
case class PartsRequest(id: Int, date: LocalDate, unavailable: List[Int], assignments: List[Assignment])

object Indexer {
  def indexByFirst[A, B](values: Seq[(A, B)]): Map[A, Seq[B]] =
    values.groupBy(_._1).map({ case(k,v) =>
    k -> v.map(_._2)
  })
}

trait SlickPlanDb extends Tables {
  import Indexer._
  import profile.api._

  type DBIOpt[A] = OptionT[DBIO, A]

  def listPlans: Future[Seq[PlanRow]] = db.run(planQuery.sortBy(_.id.desc).result)

  def getPlanRow(id: Int): DBIO[Option[PlanRow]] =
    planQuery.filter(_.id === id).result.headOption

  def getPlan(id: Int)(implicit ec: ExecutionContext): Future[Option[Plan]] = {
    db.run(
      (for {
        planRow <- OptionT(getPlanRow(id))
        services <- OptionT.liftF(serviceQuery.result: DBIO[Seq[Service]])
        serviceMap = services.map(s => s.id -> s).toMap
        scheduleRows <- OptionT.liftF(scheduleWithAssigneeResult(id))
        scheduleIds = scheduleRows.map { case (schedule, _, _) => schedule.id }
        unavailable <- OptionT.liftF(unavailableOn(scheduleIds))
      } yield {
        val schedules = scheduleRows
          .groupBy { case (schedule, _, _) => schedule }
          .map { case (schedule, groupOfDay) =>
            val assignments = groupOfDay
              .groupBy { case (_, s2s, assignee) => s2s.map(_.serviceId) }
              .collect { case (Some(serviceId), groupOfService) =>
                buildAssignment(groupOfService, serviceMap(serviceId))
              }
            Schedule(
              schedule.id,
              schedule.date,
              unavailable.getOrElse(schedule.id, Nil).toList,
              assignments.toList)
          }
          .toList
          .sortBy(_.date)
        Plan(planRow.id, planRow.name, schedules, services.toList)
      }).value
    )
  }

  private[this] def buildAssignment(
    groupOfService: Seq[(ScheduleRow, Option[Schedule2ServiceRow], Option[AssigneeRow])],
    service: Service
  ): Assignment = {
    val assignmentsByShift = groupOfService
      .collect { case (_, Some(s2s), Some(assignee)) => s2s.shift -> assignee.id }
      .toMap

    val shifts = for {
      n <- 0 until service.slots
    } yield assignmentsByShift.get(n)

    Assignment(service.id, shifts.toList)
  }

  private[this] def unavailableOn(scheduleIds: Seq[Int])(implicit ec: ExecutionContext):
    DBIO[Map[Int, Seq[Int]]] =
    unavailableQuery.filter(_.scheduleId inSet scheduleIds).result.map { indexByFirst }

  type FlatScheduleRow = (ScheduleRow, Option[Schedule2ServiceRow], Option[AssigneeRow])

  private[this] def scheduleWithAssigneeQuery(planId: Int) =
    scheduleQuery.filter(_.planId === planId)
      .joinLeft(schedule2Service).on { case (schedule, s2s) => schedule.id === s2s.scheduleId }
      .joinLeft(assigneeQuery).on { case ((_, s2s), a) => s2s.map(_.assigneeId) === a.id }
      .map { case ((schedule, s2s), assignee) => (schedule, s2s, assignee) }

  private[this] def scheduleWithAssigneeResult(planId: Int): DBIO[Seq[FlatScheduleRow]] =
    scheduleWithAssigneeQuery(planId).result

  private[this] def deleteScheduleRefs(scheduleId: Int): DBIO[(Int, Int)] =
    unavailableQuery.filter(_.scheduleId === scheduleId).delete zip
      schedule2Service.filter(_.scheduleId === scheduleId).delete

  def savePlan(plan: PlanUpdateRequest)(implicit ec: ExecutionContext):
    Future[List[Unit]] = {
    val partUpdates: List[DBIO[Unit]] = plan.parts.map { part =>
      for {
        _ <- scheduleQuery.filter(_.id === part.id).map(_.date).update(part.date)
        _ <- deleteScheduleRefs(part.id)
        _ <- unavailableQuery ++= part.unavailable.map(assigneeId => (part.id, assigneeId))
        assignments = part.assignments.flatMap { case Assignment(serviceId, shifts) =>
          shifts.zipWithIndex collect { case (Some(assigneeId), shift) =>
            Schedule2ServiceRow(assigneeId, serviceId, part.id, shift)
          }
        }
        _ <- schedule2Service ++= assignments
      } yield ()
    }
    db.run(DBIO.sequence(partUpdates).transactionally)
  }

  def removePlan(id: Int)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run {
      {
        for {
          scheduleIds <- scheduleQuery.filter(_.planId === id).map(_.id).result
          _ <- DBIO.sequence(scheduleIds.map(deleteScheduleRefs))
          _ <- scheduleQuery.filter(_.planId === id).delete
          _ <- planQuery.filter(_.id === id).delete
        } yield ()
      }.transactionally
    }
  }

  def createPlan(from: LocalDate, to: LocalDate)(implicit ec: ExecutionContext):
    Future[Int] = {
    val newPlan = PlanRow(-1, s"${from.format(dateFormat)} bis ${to.format(dateFormat)}")
    db.run {
      {
        for {
          planId <- planQuery returning planQuery.map(_.id) += newPlan
          dates = daysBetween(from, to, Seq(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))
          _ <- scheduleQuery ++= dates.map(ScheduleRow(-1, _, planId))
        } yield planId
      }.transactionally
    }
  }

  private[this] val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  private[this] def daysBetween(from: LocalDate, to: LocalDate, weekDays: Seq[DayOfWeek]):
    Stream[LocalDate] = {
    lazy val loopingWeekdays: Stream[DayOfWeek] = weekDays.toStream #::: loopingWeekdays

    // drop first day of week if from is the same DOW
    // otherwise the first `next` call would skip e.g. from Friday to Friday
    // from will be included anyway by scanLeft
    val weekdayAdjusters = loopingWeekdays
      .dropWhile(dow => dow == from.getDayOfWeek)
      .map(TemporalAdjusters.next)

    weekdayAdjusters.scanLeft(from)((date, nextWeekday) => {
      date `with` nextWeekday
    }).dropWhile(d => !weekDays.contains(d.getDayOfWeek))
      .takeWhile(d => d.isBefore(to) || d.isEqual(to))
  }
}
