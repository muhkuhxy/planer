package models.smt

import anorm._
import anorm.SqlParser._
import com.google.inject.ImplementedBy
import java.sql.Connection
import java.time._
import java.time.format._
import java.time.temporal._
import javax.inject._
import java.util.Date
import play.api.Play.current
import play.api.db._
import play.api.Logger
import scala.language.postfixOps
import scala.language.implicitConversions

case class Assignee(id: Int = -1, name: String, services: Set[String], email: Option[String] = None)

@ImplementedBy(classOf[DefaultAssigneeRepository])
trait AssigneeRepository {
  def getAssignees: List[Assignee]
  def save(helpers: List[Assignee])
}

class DefaultAssigneeRepository @Inject()(db: Database) extends AssigneeRepository with Helper {

  def getAssignees = db.withConnection { implicit c =>
    val result = SQL"""
      select v.id, v.name as volunteer, s.name as service, v.email from volunteer v
      left join volunteer_service vs on v.id = vs.volunteer_id
      left join service s on s.id = vs.service_id
      where v.active
    """.as(int("id") ~
      str("volunteer") ~
      get[Option[String]]("service") ~
      get[Option[String]]("email") map (flatten) *)
    val assignees = for {
      ((id, name), bla) <- result.groupBy(x => (x._1, x._2))
    } yield Assignee(id, name, bla.map(_._3).flatten.toSet, bla.head._4)
    assignees.toList.sortBy(_.name)
  }

  def save(helpers: List[Assignee]) = db.withConnection { implicit c =>
    val serviceIds = servicesByName
    val volunteerIds = volunteersByName
    sealed trait AssigneeOp {
      def execute()
    }
    case class Update(as: Assignee) extends AssigneeOp {
      require(as.id > 0)
      val asId = as.id
      def execute {
        Logger.info(s"updating services $as with id $asId")
        val result = SQL("update volunteer set name = {name}, email = {email} where id = {id}")
          .on("name" -> as.name, "email" -> as.email, "id" -> asId).executeUpdate()
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
    case class Remove(as: Assignee) extends AssigneeOp {
      def execute {
        Logger.info(s"removing $as")
        val geloeschtId = volunteerIds("geloescht")
        val asId = volunteerIds(as.name)
        SQL"update volunteer_service set volunteer_id = $geloeschtId where volunteer_id = $asId".executeUpdate()
        SQL"update schedule_services set volunteer_id = $geloeschtId where volunteer_id = $asId".executeUpdate()
        SQL"update unavailable set volunteer_id = $geloeschtId where volunteer_id = $asId".executeUpdate()
        val result = SQL"delete from volunteer where id = $asId".executeUpdate()
        Logger.debug(s"$result geloescht")
      }
    }
    def calculateDifferences(old: List[Assignee], now: List[Assignee]): Seq[AssigneeOp] = {
      import scala.collection.mutable.ListBuffer
      val oldMap = old.map( as => as.id -> as ).toMap
      var ops = ListBuffer.empty[AssigneeOp]
      now.foreach { case as @ Assignee(id, name, services, email) =>
        if(id > 0) {
          val existing = oldMap(id)
          if(existing.services != services || existing.email != email || existing.name != name) {
            ops += Update(as)
          }
        } else {
          ops += Add(as)
        }
      }
      val nowIds = now.map(_.id).toSet
      val oldIds = old.map(_.id).toSet
      val removeIds = oldIds.diff(nowIds).filter(_ > 0)
      ops ++= removeIds.map(oldMap).map(Remove)
      ops
    }
    val old = getAssignees
    calculateDifferences(old, helpers) foreach (_.execute())
  }

}
