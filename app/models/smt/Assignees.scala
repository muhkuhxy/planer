package models.smt

import app.Application.logger
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
import scala.language.postfixOps
import scala.language.implicitConversions

case class Assignee(id: Int = -1, name: String, services: List[Int], email: Option[String] = None)

@ImplementedBy(classOf[DefaultAssigneeRepository])
trait AssigneeRepository {
  def getAssignees: List[Assignee]
  def getServices: List[Service]
  def save(helpers: List[Assignee])
}

class DefaultAssigneeRepository @Inject()(db: Database) extends AssigneeRepository with Helper {

  def getServices = db.withConnection { implicit c =>
    services.toList
  }

  def getAssignees = db.withConnection { implicit c =>
    val result = SQL"""
      select v.id, v.name as volunteer, s.id as service, v.email from volunteer v
      left join volunteer_service vs on v.id = vs.volunteer_id
      left join service s on s.id = vs.service_id
    """.as(int("id") ~
      str("volunteer") ~
      get[Option[Int]]("service") ~
      get[Option[String]]("email") map (flatten) *)
    val assignees = for {
      ((id, name), grouped) <- result.groupBy(x => (x._1, x._2))
    } yield Assignee(id, name, grouped.flatMap(_._3).toList.sorted, grouped.head._4)
    assignees.toList.sortBy(_.name)
  }

  def save(helpers: List[Assignee]) = db.withConnection { implicit c =>
    sealed trait AssigneeOp {
      def execute()
    }
    case class Update(as: Assignee) extends AssigneeOp {
      require(as.id > 0)
      val asId = as.id
      def execute {
        logger.info(s"updating services $as with id $asId")
        val result = SQL("update volunteer set name = {name}, email = {email} where id = {id}")
          .on("name" -> as.name, "email" -> as.email, "id" -> asId).executeUpdate()
        SQL"delete from volunteer_service where volunteer_id = $asId".executeUpdate()
        for(service <- as.services)
          SQL"insert into volunteer_service values ($asId, $service)".executeInsert()
      }
    }
    case class Add(as: Assignee) extends AssigneeOp {
      def execute {
        logger.info(s"adding $as")
        val asId = SQL"insert into volunteer(name, email) values (${as.name}, ${as.email})".executeInsert()
        for (service <- as.services)
          SQL"insert into volunteer_service values ($asId, $service)".executeInsert()
      }
    }
    case class Remove(as: Assignee) extends AssigneeOp {
      def execute {
        logger.info(s"removing $as")
        val asId = as.id
        val result = SQL"""
          delete from volunteer_service where volunteer_id = $asId;
          delete from schedule_services where volunteer_id = $asId;
          delete from unavailable where volunteer_id = $asId;
          delete from volunteer where id = $asId;
        """.executeUpdate()
        logger.debug(s"$result geloescht")
      }
    }
    def calculateDifferences(old: List[Assignee], now: List[Assignee]): Seq[AssigneeOp] = {
      import scala.collection.mutable.ListBuffer
      val oldMap = old.map( as => as.id -> as ).toMap
      var ops = ListBuffer.empty[AssigneeOp]
      now.foreach { case as @ Assignee(id, name, services, email) =>
        if(id > 0) {
          val existing = oldMap(id)
          if(existing.services.toSet != services.toSet || existing.email != email || existing.name != name) {
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
