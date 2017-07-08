import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}


object WebServer extends HttpApp with App {

  def index = getFromResource("index.html")
  def css = getFromResource("CSS/main.css")
  def js = getFromResource("Javascript/main.js")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  override def routes: Route = {
    path("") {
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
    path("webSocketTest") {
      get {
        handleWebSocketMessages(webSocketHandler)
      }
    }

  }

  def webSocketHandler: Flow[Message, Message, Any] = {
    Flow[Message]
      .mapConcat {
        case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
        //case tm: TextMessage => TextMessage("Hello!") :: Nil
        //case tm: TextMessage => TextMessage("Hello " + extractUpgradeToWebSocket.toString) :: Nil

        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
  }

  WebServer.startServer("192.168.0.3", 8080)
}
