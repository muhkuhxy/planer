package models.smt

import anorm._
import anorm.SqlParser._
import java.sql.Connection
import java.time.LocalDate
import scala.language.postfixOps

trait Helper {

  implicit def localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def localDateDescOrdering: Ordering[LocalDate] = localDateOrdering.reverse

  def services(implicit c: Connection): Iterable[Service] =
    SQL"select id, name, slots from service order by id".as(Macro.namedParser[Service].*)

  def indexByFirst[A, B](values: List[(A, B)]): Map[A, List[B]] =
    values.groupBy(_._1).map({ case(k,v) =>
      k -> v.map(_._2)
    })

  val singleId = int("id").single

  val multiIds = int("id") *

  val idAndName = int("id") ~ str("name") map flatten *

  def volunteers(implicit c: Connection) =
    SQL("select id, name from volunteer").as(idAndName)

  def volunteersByName(implicit c: Connection) =
    volunteers.map(v => v._2 -> v._1).toMap
}

