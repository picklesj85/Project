package server

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import spray.json._
import server.MyJsonProtocol


class UserManager(implicit actorSystem: ActorSystem) {

  val director = actorSystem.actorOf(Props(classOf[Director]))

  def getDirector = director

}

object UserManager {

  var loggedIn: Set[String] = Set.empty[String] // list of users that have authenticated for security

  var onlineUsers: Set[User] = Set.empty[User] // current online users

  def login(userName: String) = loggedIn += userName

  def logout(userName: String) = {
    loggedIn -= userName
    
  }
}


class Director extends Actor with ActorLogging {

  override def receive = {

    case newUser: User => {
      UserManager.onlineUsers += newUser
      UserManager.onlineUsers.foreach(user => user.actorRef ! WrappedMessage(SendUser("newUser", newUser.userName).toJson.prettyPrint))
    }

    case userLeft: SendUser => {
      UserManager.onlineUsers.foreach(user => {
        if (user.userName == userLeft.userName) UserManager.onlineUsers -= user
        user.actorRef ! WrappedMessage(userLeft.toJson.prettyPrint)
      })

    }
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
