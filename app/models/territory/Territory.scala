package models.territory

import java.time.{LocalDate, Period}

case class Territory(id: String, households: Int, streets: Seq[String], log: Seq[LogEntry]) {
  def isAvailable = log.isEmpty || log.head.isInstanceOf[Return]
  def lastWorked: Option[LocalDate] = log.find(_.isWorked).map(_.date)
}

abstract class LogEntry {
  def id: Int
  def date: LocalDate
  def territory: String
  def isWorked: Boolean
}
case class Issue(id: Int, date: LocalDate, territory: String, friend: Friend) extends LogEntry {
  val isWorked = false
}
case class Return(id: Int, date: LocalDate, territory: String) extends LogEntry {
  val isWorked = true
}
case class Finished(id: Int, date: LocalDate, territory: String) extends LogEntry {
  val isWorked = true
}

case class Friend(name: String, groupId: Int)

package vm {

  case class AvailableTerritory(id: String, monthsNotWorked: BigDecimal, lastWorked: LocalDate, households: Int, streets: Seq[String])

  case object AvailableTerritory {
    def monthsNotWorked(lastWorked: LocalDate) = {
      val p = Period.between(lastWorked, LocalDate.now())
      val months = p.getYears * 12 + p.getMonths + p.getDays / 30.0
      java.math.BigDecimal.valueOf(months).setScale(1, java.math.RoundingMode.HALF_UP)
    }
    def apply(t: Territory): AvailableTerritory = {
      val lastWorked = t.lastWorked.get
      AvailableTerritory(t.id, monthsNotWorked(lastWorked), lastWorked, t.households, t.streets)
    }
  }

  case class IssuedTerritory(id: String, issued: LocalDate, friend: String)

  case object IssuedTerritory {
    def findIssued(log: Seq[LogEntry]): Issue = log.find(_.isInstanceOf[Issue]).get.asInstanceOf[Issue]

    def apply(t: Territory): IssuedTerritory = {
      val issued = findIssued(t.log)
      IssuedTerritory(t.id, issued.date, issued.friend.name)
    }
  }

}
