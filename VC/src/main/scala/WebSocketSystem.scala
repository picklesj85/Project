
import akka.actor.{Actor, ActorRef, ActorSystem, Props}


case class MyMessage(data: String)


class Room(roomID: Int, actorSystem: ActorSystem) {

  var roomMembers: Set[ActorRef] = Set.empty[ActorRef]

  val roomModerator = actorSystem.actorOf(Props(new Actor {
    override def receive = {
      case m: MyMessage => roomMembers.foreach(_ ! m)

      case ar: ActorRef => {
        roomMembers += ar
        if (roomID != Int.MaxValue) {
          ar ! MyMessage(roomID.toString)
          ar ! MyMessage(s"Welcome to room $roomID!")
        }
        else ar ! MyMessage("The Room ID entered does not exist.")
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
