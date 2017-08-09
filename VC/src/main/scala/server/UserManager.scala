package server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import spray.json._
import server.MyJsonProtocol


class OnlineUser(clientActor: ActorRef) extends Actor with ActorLogging {

  UserManager.onlineUsers += this

  override def receive = {

    case message: WrappedMessage => {
      // do another match to see what type of message. eg if poll send list of online users.
      clientActor ! message
    }

    case newUser: User => {
      UserManager.onlineUsers.foreach(user => user.self ! WrappedMessage(SendUser("newUser", newUser.userName).toJson.prettyPrint))
    }

    case userLeft: SendUser => {
      UserManager.onlineUsers.foreach(user => {
        if (user.userName == userLeft.userName) UserManager.onlineUsers -= user
        user.actorRef ! WrappedMessage(userLeft.toJson.prettyPrint)
      })

    }
  }

}

object UserManager {

  var loggedIn: Set[String] = Set.empty[String] // list of users that have authenticated for security

  var onlineUsers: Set[OnlineUser] = Set.empty[OnlineUser] // current online users

  def login(userName: String) = loggedIn += userName

  def logout(userName: String) = {
    loggedIn -= userName
    
  }
}


object test extends App {
  implicit val system = ActorSystem()
  val d1 = UserManager.director
  val d2 = UserManager.director
  println(d1.equals())
  println(d1.hashCode())
  println(d2.hashCode())
}
