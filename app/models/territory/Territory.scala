package models.territory

import java.time.{LocalDate, Period}
import com.google.inject.ImplementedBy
import summary._

case class Territory(id: String, streets: Seq[Street], bans: Seq[Ban], log: Seq[LogEntry], city: String) {
  def issue(to: Friend) = {
    require(available)
    LogEntry.issued(to)
  }

  def `return`(from: Friend) = {
    require(issued)
    LogEntry.returned(from)
  }

  def issuedTo: Option[Friend] = for {
    head <- log.headOption
    if head.kind.isInstanceOf[LogEntry.IssuedType]
    who <- head.who
  } yield who

  def available = log.headOption.map(_.kind.available).getOrElse(true)

  def issued = !available

  def state = log.headOption.map(_.kind.resultingState).getOrElse("Taufrisch")
}

case class Street(name: String, range: (String, String), households: Int)
case class Ban(name: String, address: String, date: LocalDate)
case class LogEntry(date: LocalDate, kind: LogEntry.EntryType, who: Option[Friend])

object LogEntry {
  trait EntryType {
    val name: String
    def resultingState: String
    def available: Boolean
    def description: String
  }
  trait IssuedType extends EntryType {
    def resultingState = "Ausgegeben"
    def available = false
  }
  case object Issued extends IssuedType {
    val name = "Issued"
    def description = "Ausgegeben"
  }
  case object Worked extends IssuedType {
    val name = "Worked"
    def description = "Bearbeitet"
  }
  case object Returned extends EntryType {
    val name = "Returned"
    def resultingState = "Im Kasten"
    def available = true
    def description = "Zurück"
  }

  def issued(to: Friend, date: LocalDate = LocalDate.now()) = LogEntry(date, LogEntry.Issued, Some(to))
  def returned(from: Friend, date: LocalDate = LocalDate.now()) = LogEntry(date, LogEntry.Returned, Some(from))
  def fromName(name: String) = name match {
    case Issued.name => Issued
    case Worked.name => Worked
    case Returned.name => Returned
  }
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
  def find(id: String) = all.filter(_.id == id).headOption
  def all = List(
    Territory("7120",
      List(Street("kirchrather", ("1a", "15"), 12), Street("kirchrather", ("2", "20"), 10)),
      bans = List(),
      log = List(
        LogEntry(LocalDate.of(2017, 4, 30), LogEntry.Returned, Some(Friend("irmgard", 1)))
      , LogEntry(LocalDate.of(2017, 1, 3), LogEntry.Issued, Some(Friend("reinhard", 1)))
      ),
      "Niederbardenberg"
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
        LogEntry(LocalDate.of(2016, 11, 7), LogEntry.Returned, Some(Friend("reinhard", 1)))
      , LogEntry(LocalDate.of(2016, 6, 30), LogEntry.Worked, Some(Friend("irmgard", 1)))
      , LogEntry(LocalDate.of(2016, 1, 3), LogEntry.Issued, Some(Friend("reinhard", 1)))
      ),
      "Würselen"
    )
  , Territory("1110",
      List(Street("plitscharder", ("15", "15"), 2), Street("buxtehude str.", ("2", "20"), 10)),
      bans = List(Ban("loeffen", "kirchrather 46", LocalDate.now())),
      log = List(
        LogEntry(LocalDate.of(2016, 6, 30), LogEntry.Issued, Some(Friend("sascha", 1)))
      , LogEntry(LocalDate.of(2016, 11, 7), LogEntry.Returned, Some(Friend("timon", 1)))
      , LogEntry(LocalDate.of(2016, 1, 3), LogEntry.Issued, Some(Friend("timon", 1)))
      ),
      "Kohlscheid"
    )
  )
  def addLog(l: LogEntry) = {}
}


