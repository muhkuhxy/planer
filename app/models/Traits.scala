package models

import java.time.LocalDate

case class Assignee(name: String, services: Set[String], email: Option[String] = None)
case class Plan(id: Int, name: String, parts: List[Schedule])
case class Schedule(date: LocalDate, unavailable: List[String], assignments: Map[String,List[String]])

trait AssigneeRepository {
  def getAssignees: List[Assignee]
  def save(helpers: List[Assignee])
}

trait PlanRepository {
  def save(plan: Plan)
  def list: List[Plan]
  def find(id: Long): Plan
  def remove(id: Long)
  def create(from: LocalDate, to: LocalDate): Long
}

