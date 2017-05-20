package models.territory

import java.time.{LocalDate, Period}
import com.google.inject.ImplementedBy
import summary._

case class Territory(id: String, streets: Seq[Street], bans: Seq[Ban], log: Seq[LogEntry], city: String) {
  def issue(to: Friend) = {
    require(available)
    LogEntry.issued(to)_
  }

  def issuedTo: Option[Friend] = for {
    head <- log.headOption
    if head.kind == LogEntry.Issued
    who <- head.who
  } yield who

  def issued = issuedTo.nonEmpty

  // kann sein, dass ein ding weder issued noch available ist?

  def available = log.headOption.map(_.kind == LogEntry.Returned).getOrElse(true)
}

case class Street(name: String, range: (String, String), households: Int)
case class Ban(name: String, address: String, date: LocalDate)
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

package summary {
  object Months {
    def toDecimals(date: LocalDate): BigDecimal = {
      val p = Period.between(date, LocalDate.now())
      val months = p.getYears * 12 + p.getMonths + p.getDays / 30.0
      BigDecimal(java.math.BigDecimal.valueOf(months).setScale(1, java.math.RoundingMode.HALF_UP))
    }
  }

  case class AvailableTerritory(id: String, lastWorked: Option[LocalDate], households: Int, streets: Seq[String], city: String) {
    def monthsNotWorked = lastWorked.map(Months.toDecimals)
  }
  case class IssuedTerritory(id: String, issued: LocalDate, friend: Friend, streets: Seq[String], city: String) {
    def monthsIssued = Months.toDecimals(issued)
  }
}

@ImplementedBy(classOf[InMemoryRepo])
trait TerritoryRepository {
  val availableOrdering =
    Ordering.by{ at: AvailableTerritory => (at.monthsNotWorked.isDefined, at.monthsNotWorked) }(Ordering.Tuple2(Ordering[Boolean], Ordering[Option[BigDecimal]].reverse))
  def find(id: String): Option[Territory]
  def all: Seq[Territory]
  def summary: (Seq[AvailableTerritory], Seq[IssuedTerritory]) = {
    val (available, issued) = all.partition(_.available)
    (available map { t =>
        AvailableTerritory(t.id, t.log.headOption.map(_.date), t.streets.map(_.households).sum, t.streets.map(_.name), t.city)
      } sorted(availableOrdering),
      issued map { t =>
        IssuedTerritory(t.id, t.log.head.date, t.log.head.who.get, t.streets.map(_.name), t.city)
    })
  }
  def addLog(log: LogEntry)
}

class InMemoryRepo extends TerritoryRepository {
  def find(id: String) = None
  def all = List(
    Territory("7120",
      List(Street("kirchrather", ("1a", "15"), 12), Street("kirchrather", ("2", "20"), 10)),
      bans = List(),
      log = List(
        LogEntry(LocalDate.of(2017, 4, 30), LogEntry.Returned, Some(Friend("irmgard", 1)), "")
      , LogEntry(LocalDate.of(2017, 1, 3), LogEntry.Issued, Some(Friend("reinhard", 1)), "")
      ),
      "Würselen"
    )
  , Territory("2130",
      List(Street("aachener str.", ("127", "159"), 102), Street("dürener", ("2", "20"), 10)),
      bans = List(),
      log = List(),
      "Würselen"
    )
  , Territory("6150",
      List(Street("aachener str.", ("127", "159"), 102), Street("dürener", ("2", "20"), 10)),
      bans = List(),
      log = List(
        LogEntry(LocalDate.of(2016, 11, 7), LogEntry.Returned, Some(Friend("reinhard", 1)), "")
      , LogEntry(LocalDate.of(2016, 6, 30), LogEntry.Worked, Some(Friend("irmgard", 1)), "")
      , LogEntry(LocalDate.of(2016, 1, 3), LogEntry.Issued, Some(Friend("reinhard", 1)), "")
      ),
      "Würselen"
    )
  , Territory("1110",
      List(Street("plitscharder", ("15", "15"), 2), Street("buxtehude str.", ("2", "20"), 10)),
      bans = List(Ban("loeffen", "kirchrather 46", LocalDate.now())),
      log = List(
        LogEntry.issued(Friend("timon", 1))("test")
      ),
      "Kohlscheid"
    )
  )
  def addLog(l: LogEntry) = {}
}


