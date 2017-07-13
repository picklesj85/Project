
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

class Room(roomID: Int, actorSystem: ActorSystem) {

  var roomMembers: Set[ActorRef] = Set.empty[ActorRef]

  val roomModerator = actorSystem.actorOf(Props(new Actor {
    override def receive = {
      case m: MyMessage => roomMembers.foreach(_ ! m)

      case ar: ActorRef => roomMembers += ar
    }
  }))

//  def broadcast(msg: Message) = {
//    // roomModerator ! Message(message)
//    roomMembers.foreach(_ ! msg)
//  }

//  def join(actorRef: ActorRef) = {
//    roomMembers += actorRef
//  }
}

case class MyMessage(data: String)



object OpenRooms {

  var openRooms: Map[Int, Room] = Map.empty[Int, Room]

  def findRoom(roomID: Int)(implicit actorSystem: ActorSystem) = {
    if (openRooms.contains(roomID)) openRooms(roomID)
    else {
      val newRoom = new Room(roomID, actorSystem)
      openRooms += roomID -> newRoom
      newRoom
    }
  }
}
