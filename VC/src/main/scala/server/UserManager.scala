package server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import spray.json._


class OnlineUser(userName: String) extends Actor with ActorLogging with MyJsonProtocol {

  var thisUser: ActorRef = _


  override def receive = {

    case user: User =>
      thisUser = user.actorRef
      UserManager.onlineUsers += userName -> thisUser
      thisUser ! WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint)


    case message: WrappedMessage => message.data match {

        // send client all online users
      case "poll" => thisUser ! WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint)

      case "logout" =>
        UserManager.onlineUsers -= userName
        UserManager.loggedIn -= userName
        self ! PoisonPill
    }

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


