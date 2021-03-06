package server

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import database.DBConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._


object WebServer extends HttpApp {

  def index = getFromResource("index.html")
  def room = getFromResource("room.html")
  def css = getFromResource("CSS/main.css")
  def js = getFromResource("Javascript/main.js")
  def jquery = getFromResource("Javascript/lib/jquery-3.2.1.js")
  def adapter = getFromResource("Javascript/lib/adapter.js")
  def home = getFromResource("home.html")
  def loginError = getFromResource("loginError.html")
  def createAccount = getFromResource("createAccount.html")
  def userExists = getFromResource("usernameExists.html")
  def homeJs = getFromResource("Javascript/home.js")
  def ring = getFromResource("telephone-ring-03a.mp3")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  override def routes: Route = {
    pathEndOrSingleSlash {
      get {
        index
      } ~
      post {
        val room = OpenRooms.createRoom()
        complete(room.roomID.toString)
      }
    } ~
    pathPrefix("credentials") {
      post {
        formFields('username, 'password) { (username, password) =>
          val connection = DBConnector.connect
          if (DBConnector.authenticate(username, password, connection)) {
            UserManager.login(username.toLowerCase())
            connection.close()
            redirect("home?user=" + username, StatusCodes.SeeOther)
          } else {
            connection.close()
            redirect("loginError", StatusCodes.SeeOther)
          }
        }
      }
    } ~
    pathPrefix("home") {
      parameter('user) { user =>
        if (UserManager.loggedIn.contains(user.toLowerCase())) // user has validated credentials
          get {
            home
          }
        else
          get {
            complete("You must be logged in to view this page.") // user is trying to access page without logging in
          }
      }
    } ~
    path("loginError") {
      get {
        loginError
      }
    } ~
    pathPrefix("createAccount") {
      post {
        formFields('username, 'password) { (username, password) =>
          val connection = DBConnector.connect
          if (DBConnector.createUser(username, password, connection)) {
            UserManager.login(username)
            connection.close()
            redirect("home?user=" + username, StatusCodes.SeeOther)
          } else {
            connection.close()
            redirect("userExists", StatusCodes.SeeOther)
          }
        }
      } ~
      get {
        createAccount
      }
    } ~
    pathPrefix("room") {
      get {
        room
      }
    } ~
    path("userExists") {
      get {
        userExists
      }
    } ~
    path("js") {
      get {
        js
      }
    } ~
    path("css") {
      get {
        css
      }
    } ~
    path("jquery") {
      get {
        jquery
      }
    } ~
    path("adapter") {
      get {
       adapter
      }
    } ~
    path("homeJs") {
      get {
        homeJs
      }
    } ~
    path("ring") {
      get {
        ring
      }
    } ~
    pathPrefix("webSocket" / IntNumber) {
      roomID =>
        parameter('name) { user =>
          handleWebSocketMessages(webSocketHandler(roomID, user))
        }
    } ~
    pathPrefix("loggedIn") {
      parameter('user) { user =>
        handleWebSocketMessages(loggedInSocketHandler(user))
      }
    }
  }

  def webSocketHandler(num: Int, name: String): Flow[Message, Message, Any] = {

    val room = OpenRooms.findRoom(num)
    val moderator = room.roomModerator

    val sink = Sink.actorRef[WrappedMessage](moderator, PoisonPill)

    def incoming: Sink[Message, _] = {
      Flow[Message].collect {
       case TextMessage.Strict(msg) => WrappedMessage(msg)

       case TextMessage.Streamed(stream) => Await.result(stream.runFold("")(_ + _) // handle streams
         .flatMap(Future.successful)
         .map(msg => WrappedMessage(msg)), 1000.millis)

      }.to(sink)
    }

    def outgoing: Source[Message, _] = {
      Source.actorRef[WrappedMessage](50, OverflowStrategy.dropHead)
        .mapMaterializedValue(sourceActor => moderator ! User(name, sourceActor))
        .map {
          case msg: WrappedMessage => TextMessage(msg.data)
        }
    }

    Flow.fromSinkAndSource(incoming, outgoing)

  }

  def loggedInSocketHandler(userName: String): Flow[Message, Message, Any] = {

    val user = system.actorOf(Props(new OnlineUser(userName)))

    val sink = Sink.actorRef[WrappedMessage](user, PoisonPill)

    def incoming: Sink[Message, _] = {
      Flow[Message].collect {

        case TextMessage.Strict(msg) => WrappedMessage(msg)

      }.to(sink)
    }

    def outgoing: Source[Message, _] = {
      Source.actorRef[WrappedMessage](10, OverflowStrategy.dropHead)
        .mapMaterializedValue(sourceActor => user ! User(userName, sourceActor))
        .map {
          case msg: WrappedMessage => TextMessage(msg.data)
        }
    }

    Flow.fromSinkAndSource(incoming, outgoing)

  }

  def main(args: Array[String]): Unit = {
    WebServer.startServer("172.20.10.3", 8080)
    system.terminate()
  }

}


