import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}


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
    }

  }

  WebServer.startServer("localhost", 8080)
}
