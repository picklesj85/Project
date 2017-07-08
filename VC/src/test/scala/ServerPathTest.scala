
import org.scalatest._
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.BinaryMessage
import scala.io.Source
import akka.util.ByteString
import scala.concurrent.duration._

class ServerPathTest extends FunSuite with Matchers with ScalatestRouteTest {

  val ws = WebServer.routes

  val webSocketClient = WSProbe() // represents the client side of the web socket


  test("root path returns index.html") {
    Get() ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("index.html").mkString
    }
  }

  test("js path returns main.js") {
    Get("/js") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("Javascript/main.js").mkString
    }
  }

  test("css path returns main.css") {
    Get("/css") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("CSS/main.css").mkString
    }
  }

  test("test the webSocketTest") {
    WS("/webSocketTest", webSocketClient.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      webSocketClient.sendMessage("James")
      webSocketClient.expectMessage("Hello James!")

      webSocketClient.sendMessage("Sarah")
      webSocketClient.expectMessage("Hello Sarah!")

      webSocketClient.sendMessage(BinaryMessage(ByteString("12345")))
      webSocketClient.expectNoMessage(100.millis)

//      webSocketClient.sendCompletion()
//      webSocketClient.expectCompletion()

    }
  }

}
