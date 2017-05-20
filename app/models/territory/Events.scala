package models.territory

import java.time.LocalDate

abstract sealed class Event {
  // def id: String
}

trait EventStore {
  def events: Stream[Event]
  // def find(id: String): Option[Event] = events.filter{ _.id == id }.headOption
  def store(e: Event): Boolean
}

case object InMemoryEventStore extends EventStore {
  val es = Stream[Event]()
  def events = es
  def store(e: Event) = {
    e #:: es
    true
  }
}

case class Issued(territory: String, friend: String) extends Event
case class Returned(territory: String) extends Event
// case class IssuedTemporarily(territory: Territory) extends Event

class CommandHandler(es: EventStore) {

  def issue(t: domain.Territory, friend: String) {
    val issued = t.issue(friend)
    es.store(Issued(t.id, friend))
  }
}

package domain {
  case class Territory(id: String, streets: Seq[String], bans: Seq[Ban], issuedTo: Option[String]) {
    def issue(to: String) = {
      require(notIssued)
      copy(issuedTo = Some(to))
    }
    def notIssued = issuedTo.isEmpty
    def issued = issuedTo.nonEmpty
  }
  case class Ban(address: Address, date: LocalDate)
  case class Address(name: String, address: String)
}

