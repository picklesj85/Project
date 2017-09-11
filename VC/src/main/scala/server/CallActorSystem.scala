package server



import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import DefaultJsonProtocol._

import scala.concurrent.duration._
import server._

import scala.util.Random




case class WrappedMessage(data: String)
case class User(userName: String, actorRef: ActorRef)
case class SendUser(tag: String, userName: String) // cannot use spray-json with User because of ActorRef
case class RoomID(tag: String, roomID: Int, caller: Boolean)
case class RoomError(tag: String)


case class Room(roomID: Int, actorSystem: ActorSystem) {

  val roomModerator = actorSystem.actorOf(Props(classOf[RoomModerator], this))

}

class RoomModerator(room: Room) extends Actor with ActorLogging with MyJsonProtocol {

  import context._

  var connected = false
  var roomMembers: Set[User] = Set.empty[User]
  val roomID = room.roomID
  val hangUp = "{\"tag\":\"hangUp\"}" // the string that a hangup message will come as

  override def receive = {
    case m: WrappedMessage =>
      //log.info(m.data)
      if (m.data == "connected") connected = true

      roomMembers.foreach(member => {
        member.actorRef ! m
        log.info("Sent to " + member.userName + ": " + m.data)
      })
      if (!connected) system.scheduler.scheduleOnce(500.millis, self, m) // keep sending each message until connected
                                                                         // in case of undelivered messages
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

  val Max = 1000000

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
    var ID = 0
    do {
      ID = Random.nextInt(Max)
    } while (openRooms.contains(ID)) // generate a random positive ID and make sure it's not in use

    val newRoom = Room(ID, actorSystem)
    openRooms += ID -> newRoom
    newRoom
  }

  def deleteRoom(roomID: Int) = openRooms -= roomID
}




