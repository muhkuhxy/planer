package services

import java.time.LocalDate

import cats.data._
import cats.implicits._
import javax.inject._
import models.domainError._
import models.smt._
import play.api.db.slick._

import scala.concurrent._

class PlanService @Inject()
  (val dbConfigProvider: DatabaseConfigProvider)
  (implicit ec: ExecutionContext)
  extends SlickPlanDb {

  def list: Future[Seq[PlanRow]] = listPlans

  def get(planId: Int): EitherT[Future, DomainError, Plan] =
    EitherT {
      getPlan(planId).map {
        _.toRight(NoPlan)
      }
    }

  def create(from: LocalDate, to: LocalDate): EitherT[Future, DomainError, Int] =
    if (from.isBefore(to)) {
      EitherT.right(createPlan(from, to))
    } else {
      EitherT.leftT(InvalidDateRange)
    }

  def save(plan: PlanUpdateRequest): Future[List[Unit]] =
    savePlan(plan)

  def delete(planId: Int): Future[Unit] =
    removePlan(planId)
}

