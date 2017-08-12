package server



import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import DefaultJsonProtocol._



case class WrappedMessage(data: String)
case class User(userName: String, actorRef: ActorRef)
case class SendUser(tag: String, userName: String) // cannot use spray-json with User because of ActorRef
case class RoomID(tag: String, roomID: Int, caller: Boolean)
case class RoomError(tag: String)



trait MyJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val sendUserFormat = jsonFormat2(SendUser)
  implicit val roomIDFormat = jsonFormat3(RoomID)
  implicit val roomErrorFormat = jsonFormat1(RoomError)
  implicit val wrappedMessageFormat = jsonFormat1(WrappedMessage)
  implicit val allOnlineUsers = jsonFormat2(AllOnlineUsers)
  implicit val roomNumber = jsonFormat2(RoomNumber)
  implicit val receiveCall = jsonFormat3(ReceiveCall)
  implicit val sendCall = jsonFormat2(SendCall)
  implicit val accepted = jsonFormat1(Accepted)
  implicit val rejected = jsonFormat1(Rejected)
}


case class Room(roomID: Int, actorSystem: ActorSystem) {

  val roomModerator = actorSystem.actorOf(Props(classOf[RoomModerator], this))

}

class RoomModerator(room: Room) extends Actor with ActorLogging with MyJsonProtocol {

  var roomMembers: Set[User] = Set.empty[User]
  val roomID = room.roomID
  val hangUp = "{\"tag\":\"hangUp\"}" // the string that a hangup message will come as

  override def receive = {
    case m: WrappedMessage =>
      //log.info(m.data)
      roomMembers.foreach(member => {
        member.actorRef ! m
        log.info("Sent to " + member.userName + ": " + m.data)
      })
      if (m.data == hangUp) {
        roomMembers.foreach(member => {
          member.actorRef ! PoisonPill
          log.info("PoisonPill sent to " + member.userName)
        }) // terminate connection
        OpenRooms.deleteRoom(roomID)
        log.info("Deleting room " + roomID)
        self ! PoisonPill
      }


    case user: User =>
      if (roomID != Int.MinValue) {
        roomMembers.foreach(member => user.actorRef ! WrappedMessage(SendUser("user", member.userName).toJson.prettyPrint))
        roomMembers += user
        roomMembers.foreach(m => log.info(m.userName + " is in the room."))
        user.actorRef ! WrappedMessage(RoomID("roomID", roomID, roomMembers.size == 1).toJson.prettyPrint)
      }
      else user.actorRef ! WrappedMessage(RoomError("roomError").toJson.prettyPrint)

  }
}


object OpenRooms {

  var IDcount = 0

  var openRooms: Map[Int, Room] = Map.empty[Int, Room]

  def findRoom(roomID: Int)(implicit actorSystem: ActorSystem) = {
    if (openRooms.contains(roomID)) openRooms(roomID)
    else {
      // create a fake room to relay message that the room ID entered is invalid.
      val roomError = Room(Int.MinValue, actorSystem)
      roomError
    }
  }

  def createRoom()(implicit actorSystem: ActorSystem) = {
    IDcount += 1
    val newRoom = Room(IDcount, actorSystem)
    openRooms += IDcount -> newRoom
    newRoom
  }

  def deleteRoom(roomID: Int) = openRooms -= roomID
}




