package server

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}


object UserManager {

  var onlineUsers: Set[User] = Set.empty[User] // current online users

  var loggedIn: Set[String] = Set.empty[String] // list of users that have authenticated for security

  def login(userName: String) = loggedIn += userName

  def logout(userName: String) = loggedIn -= userName

}
