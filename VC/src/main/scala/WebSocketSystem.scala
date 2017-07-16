
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive

import scala.util.parsing.json.JSON


case class MyMessage(data: String)
case class User(name: String, actorRef: ActorRef)

case class Room(roomID: Int, actorSystem: ActorSystem) {

  val roomModerator = actorSystem.actorOf(Props(classOf[RoomModerator], this))

}

class RoomModerator(room: Room) extends Actor with ActorLogging {

  var roomMembers: Set[User] = Set.empty[User]
  val roomID = room.roomID

  override def receive = {
    LoggingReceive {
      case m: MyMessage => roomMembers.foreach(member => member.actorRef ! m)
        log.info(m.data)

      case user: User => {
        if (roomID != Int.MaxValue) {
          roomMembers.foreach(member => user.actorRef ! MyMessage("user" + member.name))
          roomMembers += user
          user.actorRef ! MyMessage("ID" + roomID.toString)
          user.actorRef ! MyMessage(s"Welcome to Room $roomID!")
        }
        else user.actorRef ! MyMessage("The Room ID entered does not exist.")
      }
    }
  }
}


object OpenRooms {

  var IDcount = 0

  var openRooms: Map[Int, Room] = Map.empty[Int, Room]

  def findRoom(roomID: Int)(implicit actorSystem: ActorSystem) = {
    if (openRooms.contains(roomID)) openRooms(roomID)
    else {
      // create a fake room to relay message that the room ID entered is invalid.
      val fakeRoom = Room(Int.MaxValue, actorSystem)
      fakeRoom
    }
  }

  def createRoom()(implicit actorSystem: ActorSystem) = {
    IDcount += 1
    val newRoom = Room(IDcount, actorSystem)
    openRooms += IDcount -> newRoom
    newRoom
  }
}
