import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.scaladsl.{Sink, Flow, Source}


object WebServer extends HttpApp with App {

  def index = getFromResource("index.html")
  def css = getFromResource("CSS/main.css")
  def js = getFromResource("Javascript/main.js")

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
        case tm: TextMessage => TextMessage.Streamed(Source.single("Hello ") ++ tm.textStream) :: Nil
      }
  }

  WebServer.startServer("192.168.0.3", 8080)
}
