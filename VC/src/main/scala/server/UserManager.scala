package server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class OnlineUser(userName: String) extends Actor with ActorLogging with MyJsonProtocol {

  import context._

  var thisUser: ActorRef = _

  if (UserManager.onCall.contains(userName)) UserManager.onCall -= userName // no longer on a call so remove

  def available = UserManager.loggedIn -- UserManager.onCall

  override def receive = {

    case user: User =>
      if (!UserManager.loggedIn.contains(userName))  { // user has not authenticated
        self ! PoisonPill
      } else {
        thisUser = user.actorRef
        UserManager.onlineUsers += userName -> thisUser
        thisUser ! WrappedMessage(AllOnlineUsers("onlineUsers", available).toJson.prettyPrint)
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
        UserManager.onlineUsers(callee) ! WrappedMessage(ReceiveCall("receiveCall", userName, room).toJson.prettyPrint) // send call request to callee
        thisUser ! WrappedMessage(SendCall("sendCall", room).toJson.prettyPrint)

      case accept if accept.take(8) == "accepted" =>
        val caller = accept.drop(8)
        UserManager.onlineUsers(caller) ! WrappedMessage(Accepted("accepted").toJson.prettyPrint)

      case reject if reject.take(8) == "rejected" =>
        val caller = reject.drop(8)
        UserManager.onlineUsers(caller) ! WrappedMessage(Rejected("rejected").toJson.prettyPrint)

      case "onCall" => UserManager.onCall += userName
    }

    case Poll =>
      thisUser ! WrappedMessage(AllOnlineUsers("onlineUsers", available).toJson.prettyPrint)
      system.scheduler.scheduleOnce(3000.millis, self, Poll) // poll loop

  }


}




object UserManager {

  var loggedIn: Set[String] = Set.empty[String] // list of users that have authenticated for security

  var onlineUsers: Map[String, ActorRef] = Map.empty[String, ActorRef] // current online users
  // use a map so that an update with username already in will just update the ActorRef

  var onCall: Set[String] = Set.empty[String] // users currently logged in but already on a call

  def login(userName: String) = loggedIn += userName

  def logout(userName: String) = loggedIn -= userName
    

}

case class AllOnlineUsers(tag: String, onlineUsers: Set[String])
case class ReceiveCall(tag: String, user: String, room: Int)
case class Accepted(tag: String)
case class Rejected(tag: String)
case class SendCall(tag: String, room: Int)
case object Poll
case class RoomNumber(tag: String, number: Int)


