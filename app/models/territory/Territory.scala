package models.territory

import java.time.{LocalDate, Period}
import com.google.inject.ImplementedBy
import summary._

@ImplementedBy(classOf[InMemoryRepo])
trait TerritoryRepository {
  def find(id: String): Option[Territory]
  def all: Seq[Territory]
  def summary: (Seq[AvailableTerritory], Seq[IssuedTerritory]) = {
    val (available, issued) = all.partition(_.available)
    (available map { t =>
        AvailableTerritory(t.id, t.log.lastOption.map(_.date), t.streets.map(_.households).sum, t.streets.map(_.name))
        }, issued map { t =>
        IssuedTerritory(t.id, t.log.last.date, t.log.last.who.get)
      })
  }
}

class InMemoryRepo extends TerritoryRepository {
  def find(id: String) = None
  def all = List()
}

case class LogEntry(date: LocalDate, kind: LogEntry.EntryType, who: Option[Friend], note: String)

object LogEntry {
  trait EntryType {
    def name: String
  }
  case object Issued extends EntryType { def name = "Issued" }
  case object Returned extends EntryType { def name = "Returned" }
  case object Worked extends EntryType { def name = "Worked" }

  def issued(to: Friend)(note: String) = LogEntry(LocalDate.now(), LogEntry.Issued, Some(to), note)
}

case class Territory(id: String, streets: Seq[Street], bans: Seq[Ban], log: Seq[LogEntry]) {
  def issue(to: Friend) = {
    require(available)
    LogEntry.issued(to)_
  }

  def issuedTo: Option[Friend] = for {
    last <- log.lastOption
    if last.kind == LogEntry.Issued
    who <- last.who
  } yield who

  def issued = issuedTo.nonEmpty

  def available = log.lastOption.map(_.kind == LogEntry.Returned).getOrElse(true)
}

case class Street(name: String, range: (String, String), households: Int)
case class Ban(address: Address, date: LocalDate)
case class Address(name: String, address: String)

case class Friend(name: String, group: Int)

package summary {
  case class AvailableTerritory(id: String, lastWorked: Option[LocalDate], households: Int, streets: Seq[String]) {
    def monthsNotWorked = lastWorked.map { lastWorked =>
      val p = Period.between(lastWorked, LocalDate.now())
      val months = p.getYears * 12 + p.getMonths + p.getDays / 30.0
      BigDecimal(java.math.BigDecimal.valueOf(months).setScale(1, java.math.RoundingMode.HALF_UP))
    }
  }
  case class IssuedTerritory(id: String, issued: LocalDate, friend: Friend)
}

