package server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import database.DBConnector
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class OnlineUser(userName: String) extends Actor with ActorLogging with MyJsonProtocol {

  import context._

  var thisUser: ActorRef = _
  var contacts: Set[String] = _
  var pending: Set[String] = _

  if (UserManager.onCall.contains(userName)) UserManager.onCall -= userName // no longer on a call so remove

  override def receive = {

    case user: User =>
      if (!UserManager.loggedIn.contains(userName))  { // user has not authenticated
        self ! PoisonPill
      } else {
        thisUser = user.actorRef
        updateContacts
        UserManager.onlineUsers += userName -> thisUser
        sendUpdate
        thisUser ! WrappedMessage(PendingContacts("pending", getOnlineContacts).toJson.prettyPrint)
        system.scheduler.scheduleOnce(3000.millis, self, Poll) // initiate timer system to update who's online
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

      // case searchContacts
      // case respond (e.g. respondAccept or respondDecline) write to DB and updateContacts

    }

    case Poll =>
      sendUpdate
      system.scheduler.scheduleOnce(3000.millis, self, Poll) // poll loop
  }

  def available = UserManager.loggedIn diff UserManager.onCall

  def getOnlineContacts = available intersect contacts

  def getOfflineContacts = contacts diff getOnlineContacts

  def sendUpdate = {
    thisUser ! WrappedMessage(OnlineContacts("online", getOnlineContacts).toJson.prettyPrint)
    thisUser ! WrappedMessage(OfflineContacts("offline", getOfflineContacts).toJson.prettyPrint)

  }

  def updateContacts = {
    val connection = DBConnector.connect
    contacts = DBConnector.getMyContacts(userName, connection)
    pending = DBConnector.getPendingContacts(userName, connection)
    thisUser ! WrappedMessage(PendingContacts("pending", getOnlineContacts).toJson.prettyPrint)
    connection.close()
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

case class OnlineContacts(tag: String, onlineContacts: Set[String])
case class OfflineContacts(tag: String, offlineContacts: Set[String])
case class PendingContacts(tag: String, pendingContacts: Set[String])
case class ReceiveCall(tag: String, user: String, room: Int)
case class Accepted(tag: String)
case class Rejected(tag: String)
case class SendCall(tag: String, room: Int)
case object Poll
case class RoomNumber(tag: String, number: Int)


