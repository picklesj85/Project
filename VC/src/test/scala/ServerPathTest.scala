
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

class ServerPathTest extends FunSuite with Matchers with ScalatestRouteTest with BeforeAndAfterAll {


  def ws = WebServer.routes

  val wsClient1Room1 = WSProbe() // represents the client side of the web socket
  val wsClient2Room1 = WSProbe()
  val wsClient1Room2 = WSProbe()
  val wsClient2Room2 = WSProbe()
  val wsClient3Room2 = WSProbe()

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
      responseAs[String] shouldEqual Source.fromResource("Javascript/lib/jquery-3.2.1.js").mkString
    }
  }

  test("test the webSocket path 0 to create new room") {
    WS("/webSocket/0?name=Alice", wsClient1Room1.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient1Room1.expectMessage("1")

      wsClient1Room1.expectMessage("Welcome to Room 1!")

      assert(OpenRooms.openRooms.contains(1))

      wsClient1Room1.sendMessage("Test")
      wsClient1Room1.expectMessage("Test")

      wsClient1Room1.sendMessage(BinaryMessage(ByteString("12345")))
      wsClient1Room1.expectNoMessage(100.millis)

    }
  }

  test("different client joining same room") {
    WS("/webSocket/1?name=James", wsClient2Room1.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient2Room1.expectMessage("userAlice")

      wsClient2Room1.expectMessage("1")
      wsClient1Room1.expectNoMessage(100.millis)

      wsClient2Room1.expectMessage("Welcome to Room 1!")
      wsClient1Room1.expectNoMessage(100.millis)

      wsClient2Room1.sendMessage("hello")
      wsClient2Room1.expectMessage("hello")
      wsClient1Room1.expectMessage("hello")

    }
  }

  test("client joining non existing room") {
    val webSocketClient = WSProbe()
    WS("/webSocket/5008?name=test", webSocketClient.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      webSocketClient.expectMessage("The Room ID entered does not exist.")

      webSocketClient.sendMessage("Test")
      webSocketClient.expectNoMessage(100.millis)
    }
  }

  test("client creates another new room") {
    WS("/webSocket/0?name=Sam", wsClient1Room2.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient1Room2.expectMessage("2")

      wsClient1Room2.expectMessage("Welcome to Room 2!")

      assert(OpenRooms.openRooms.contains(2))

      wsClient1Room2.sendMessage("Test")
      wsClient1Room2.expectMessage("Test")

    }
  }

  test("client joins room 2") {
    WS("/webSocket/2?name=Fred", wsClient2Room2.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient2Room2.expectMessage("userSam")

      wsClient2Room2.expectMessage("2")
      wsClient1Room2.expectNoMessage(100.millis)

      wsClient2Room2.expectMessage("Welcome to Room 2!")
      wsClient1Room2.expectNoMessage(100.millis)

      assert(OpenRooms.openRooms.size == 2) // should now be 2 rooms

      wsClient2Room2.sendMessage("Test")
      wsClient2Room2.expectMessage("Test")
      wsClient1Room2.expectMessage("Test")

    }
  }

  test("three clients in a room") {
    WS("/webSocket/2?name=Sarah", wsClient3Room2.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient3Room2.expectMessage("userSam")
      wsClient3Room2.expectMessage("userFred")

      wsClient3Room2.expectMessage("2")
      wsClient1Room2.expectNoMessage(100.millis)
      wsClient2Room2.expectNoMessage(100.millis)

      wsClient3Room2.expectMessage("Welcome to Room 2!")
      wsClient1Room2.expectNoMessage(100.millis)
      wsClient2Room2.expectNoMessage(100.millis)

      wsClient3Room2.sendMessage("Talking to two other clients")
      wsClient3Room2.expectMessage("Talking to two other clients")
      wsClient2Room2.expectMessage("Talking to two other clients")
      wsClient1Room2.expectMessage("Talking to two other clients")

      wsClient2Room2.sendMessage("checking client 2 can reach everyone")
      wsClient1Room2.expectMessage("checking client 2 can reach everyone")
      wsClient3Room2.expectMessage("checking client 2 can reach everyone")
      wsClient2Room2.expectMessage("checking client 2 can reach everyone")

      wsClient1Room2.sendMessage("checking client 1 can reach everyone")
      wsClient1Room2.expectMessage("checking client 1 can reach everyone")
      wsClient3Room2.expectMessage("checking client 1 can reach everyone")
      wsClient2Room2.expectMessage("checking client 1 can reach everyone")

    }
  }

}
