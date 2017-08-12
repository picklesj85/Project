package server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class OnlineUser(userName: String) extends Actor with ActorLogging with MyJsonProtocol {

  import context._

  var thisUser: ActorRef = _


  override def receive = {

    case user: User =>
      if (!UserManager.loggedIn.contains(userName))  { // user has not authenticated
        self ! PoisonPill
      } else {
        thisUser = user.actorRef
        UserManager.onlineUsers += userName -> thisUser
        thisUser ! WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint)
        system.scheduler.scheduleOnce(3000.millis, self, Poll) // initiate timer system to update the client
      }

    case message: WrappedMessage => message.data match {

      case "logout" =>
        UserManager.onlineUsers -= userName
        UserManager.loggedIn -= userName
        self ! PoisonPill

      case "createRoom" =>
        val room = OpenRooms.createRoom()
        thisUser ! WrappedMessage(RoomNumber("roomNumber", room.roomID).toJson.prettyPrint)

      case call if call.take(4) == "call" =>
        val callee = call.drop(4) // name of user to call
        val room = OpenRooms.createRoom().roomID // room id for call
        UserManager.onlineUsers(callee) ! WrappedMessage(ReceiveCall("receiveCall", room).toJson.prettyPrint) // send call request to callee
        thisUser ! WrappedMessage(SendCall("sendCall", room).toJson.prettyPrint) // send room details to caller
    }

    case Poll =>
      thisUser ! WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint)
      system.scheduler.scheduleOnce(3000.millis, self, Poll) // poll loop
  }


}




object UserManager {

  var loggedIn: Set[String] = Set.empty[String] // list of users that have authenticated for security

  var onlineUsers: Map[String, ActorRef] = Map.empty[String, ActorRef] // current online users
  // use a map so that an update with username already in will just update the ActorRef

  def login(userName: String) = loggedIn += userName

  def logout(userName: String) = loggedIn -= userName
    

}

case class AllOnlineUsers(tag: String, onlineUsers: Set[String])
case class SendCall(tag: String, room: Int)
case class ReceiveCall(tag: String, room: Int)
case object Poll
case class RoomNumber(tag: String, number: Int)


