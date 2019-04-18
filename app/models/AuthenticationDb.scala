package models

import play.api.db.slick._
import slick.jdbc.JdbcProfile
import slick.basic._

final case class UserRow(name: String, password: String)

trait AuthenticationDb extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  lazy val users = TableQuery[UserTable]

  def passwordFor(name: String): DBIO[Option[String]] =
    users.filter(_.name === name).result.headOption

  class UserTable(tag: Tag) extends Table[String](tag, "appuser") {
    def name = column[String]("username")
    def password = column[String]("password")

    def * = password
  }
}
