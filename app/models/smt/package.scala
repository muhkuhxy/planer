package models.smt

import anorm._
import anorm.SqlParser._
import java.sql.Connection
import scala.language.postfixOps

trait Helper {

  def indexByFirst[A, B](values: List[(A, B)]): Map[A, List[B]] =
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

