import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.server.Directives._


object WebServer extends HttpApp {

  def index = getFromResource("index.html")
  def css = getFromResource("CSS/main.css")
  def js = getFromResource("Javascript/main.js")
  def jquery = getFromResource("Javascript/jquery-3.2.1.js")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  override def routes: Route = {
    pathEndOrSingleSlash {
      get {
        index
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
    pathPrefix("webSocket" / IntNumber) {
      roomID => handleWebSocketMessages(webSocketHandler(roomID))
    }
  }

  def webSocketHandler(num: Int): Flow[Message, Message, Any] = {

    val room = if (num == 0) OpenRooms.createRoom() else OpenRooms.findRoom(num)
    val moderator = room.roomModerator

    val sink = Sink.actorRef[MyMessage](moderator, PoisonPill) // is PoisonPill best option here?

    def incoming: Sink[Message, NotUsed] = {
      Flow[Message].map {
        case TextMessage.Strict(msg) => MyMessage(msg)
      }.to(sink)
    }

    def outgoing: Source[Message, _] = {
      Source.actorRef[MyMessage](10, OverflowStrategy.fail)
        .mapMaterializedValue(sourceActor => moderator ! sourceActor)
        .map {
          case msg: MyMessage => TextMessage(msg.data)
        }
    }

    Flow.fromSinkAndSource(incoming, outgoing)

  }

  def main(args: Array[String]): Unit = {
    WebServer.startServer("192.168.0.13", 8080)
    system.terminate()
  }

}


