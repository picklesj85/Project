import akka.NotUsed
import akka.actor.{ActorLogging, ActorSystem, PoisonPill}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.server.Directives._

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._


object WebServer extends HttpApp {

  def index = getFromResource("index.html")
  def room = getFromResource("room.html")
  def css = getFromResource("CSS/main.css")
  def js = getFromResource("Javascript/main.js")
  def jquery = getFromResource("Javascript/lib/jquery-3.2.1.js")
  def adapter = getFromResource("Javascript/lib/adapter.js")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  override def routes: Route = {
    pathEndOrSingleSlash {
      get {
        index
      }
    } ~
    pathPrefix("room") {
      get {
        room
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
    pathPrefix("webSocket" / IntNumber) {
      roomID =>
        parameter('name) { user =>
          handleWebSocketMessages(webSocketHandler(roomID, user))
        }
    }
  }

  def webSocketHandler(num: Int, name: String): Flow[Message, Message, Any] = {

    OpenRooms.createRoom() // here for testing so I don't need to constantly create one.

    val room = OpenRooms.findRoom(num)
    val moderator = room.roomModerator

    val sink = Sink.actorRef[WrappedMessage](moderator, PoisonPill) // is PoisonPill best option here?

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

  def main(args: Array[String]): Unit = {
    WebServer.startServer("192.168.0.21", 8080)
    system.terminate()
  }

}


