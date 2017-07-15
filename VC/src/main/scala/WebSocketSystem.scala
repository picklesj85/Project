
import akka.actor.{Actor, ActorRef, ActorSystem, Props}


case class MyMessage(data: String)
case class User(name: String, actorRef: ActorRef)


class Room(roomID: Int, actorSystem: ActorSystem) {

  var roomMembers: Set[User] = Set.empty[User]

  val roomModerator = actorSystem.actorOf(Props(new Actor {
    override def receive = {
      case m: MyMessage => roomMembers.foreach(member => member.actorRef ! m)

      case user: User => {
        if (roomID != Int.MaxValue) {
          roomMembers.foreach(member => user.actorRef ! MyMessage("user" + member.name))
          roomMembers += user
          user.actorRef ! MyMessage(roomID.toString)
          user.actorRef ! MyMessage(s"Welcome to room $roomID!")
        }
        else user.actorRef ! MyMessage("The Room ID entered does not exist.")
      }
    }
  }))

  def getID = roomID
}


object OpenRooms {

  var IDcount = 0

  var openRooms: Map[Int, Room] = Map.empty[Int, Room]

  def findRoom(roomID: Int)(implicit actorSystem: ActorSystem) = {
    if (openRooms.contains(roomID)) openRooms(roomID)
    else {
      // create a fake room to relay message that the room ID entered is invalid.
      val fakeRoom = new Room(Int.MaxValue, actorSystem)
      fakeRoom
    }
  }

  def createRoom()(implicit actorSystem: ActorSystem) = {
    IDcount += 1
    val newRoom = new Room(IDcount, actorSystem)
    openRooms += IDcount -> newRoom
    newRoom
  }
}
