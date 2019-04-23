package services

import cats.data._
import models.smt._
import scala.concurrent._
import java.time.LocalDate
import javax.inject._
import models.DomainError._
import play.api.db.slick._
import slick.basic._
import slick.jdbc.JdbcProfile

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

  def create(from: LocalDate, to: LocalDate): Future[Int] =
    createPlan(from, to)

  def save(plan: PlanUpdateRequest): Future[List[Unit]] =
    savePlan(plan)

  def delete(planId: Int): Future[Unit] =
    removePlan(planId)
}
