package models.territory

import com.google.inject.ImplementedBy

case class Friend(name: String, group: Int)

@ImplementedBy(classOf[InMemoryFriendRepo])
trait FriendRepository {
  def all: Seq[Friend]
}

class InMemoryFriendRepo extends FriendRepository {
  def all = List(Friend("Timon", 1), Friend("Reinhard", 1), Friend("Ben", 2))
}
