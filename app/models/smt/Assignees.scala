package models.smt

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

case class Assignee(name: String, services: Set[String], email: Option[String] = None)

trait AssigneeRepository {
  def getAssignees: List[Assignee]
  def save(helpers: List[Assignee])
}

object DefaultAssigneeRepository extends AssigneeRepository with Helper {

  def getAssignees = DB.withConnection { implicit c =>
    val result = SQL"""
      select v.name as volunteer, s.name as service, v.email from volunteer v
      left join volunteer_service vs on v.id = vs.volunteer_id
      left join service s on s.id = vs.service_id
    """.as(str("volunteer") ~
      get[Option[String]]("service") ~
      get[Option[String]]("email") map (flatten) *)
    val assignees = for {
      (name, bla) <- result.groupBy(_._1)
    } yield Assignee(name, bla.map(_._2).flatten.toSet, bla.head._3)
    assignees.toList.sortBy(_.name)
  }

  def save(helpers: List[Assignee]) = DB.withConnection { implicit c =>
    val serviceIds = servicesByName
    val volunteerIds = volunteersByName
    sealed trait AssigneeOp {
      def execute()
    }
    case class Update(as: Assignee) extends AssigneeOp {
      val asId = volunteerIds(as.name)
      def execute {
        Logger.info(s"updating services $as with id $asId")
        val result = SQL("update volunteer set email = {email} where id = {id}")
          .on("email" -> as.email, "id" -> asId).executeUpdate()
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
      now.foreach { case as @ Assignee(name, services, email) =>
        if(byName.contains(name)) {
          val existing = byName(name)
          if(existing.services != services || existing.email != email) {
            ops += Update(as)
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
