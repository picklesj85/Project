
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import org.scalatest._
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.ActorMaterializer

import scala.io.Source
import akka.util.ByteString

import scala.concurrent.duration._

class ServerPathTest extends FunSuite with Matchers with ScalatestRouteTest {

  def ws = WebServer.routes


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

  test("jquery path returns jquery") {
    Get("/jquery") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("Javascript/jquery-3.2.1.js").mkString
    }
  }

  test("test the webSocket path 0 to create new room") {
    val webSocketClient = WSProbe() // represents the client side of the web socket
    WS("/webSocket/0", webSocketClient.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      webSocketClient.expectMessage("1")

      webSocketClient.expectMessage("Welcome to room 1!")

      assert(OpenRooms.openRooms.contains(1))

      webSocketClient.sendMessage("Test")
      webSocketClient.expectMessage("Test")

      webSocketClient.sendMessage(BinaryMessage(ByteString("12345")))
      webSocketClient.expectNoMessage(100.millis)

    }
  }

  test("different client joining same room") {
    val webSocketClient = WSProbe()
    WS("/webSocket/1", webSocketClient.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      webSocketClient.expectMessage("1")

      webSocketClient.expectMessage("Welcome to room 1!")

      webSocketClient.sendMessage("")
      webSocketClient.expectMessage("")
    }
  }

  test("client joining non existing room") {
    val webSocketClient = WSProbe()
    WS("/webSocket/5008", webSocketClient.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      webSocketClient.expectMessage("The Room ID entered does not exist.")

      webSocketClient.sendMessage("Test")
      webSocketClient.expectNoMessage(100.millis)
    }
  }

  test("client creates another new room") {
    val webSocketClient = WSProbe()
    WS("/webSocket/0", webSocketClient.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      webSocketClient.expectMessage("2")

      webSocketClient.expectMessage("Welcome to room 2!")

      assert(OpenRooms.openRooms.contains(2))

      webSocketClient.sendMessage("Test")
      webSocketClient.expectMessage("Test")

    }
  }

  test("client joins room 2") {
    val webSocketClient = WSProbe()
    WS("/webSocket/2", webSocketClient.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      webSocketClient.expectMessage("2")

      webSocketClient.expectMessage("Welcome to room 2!")

      assert(OpenRooms.openRooms.size == 2) // should now be 2 rooms

      webSocketClient.sendMessage("Test")
      webSocketClient.expectMessage("Test")

    }
  }

}
