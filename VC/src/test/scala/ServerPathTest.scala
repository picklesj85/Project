
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import org.scalatest._
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.ActorMaterializer

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport


import scala.io.Source
import akka.util.ByteString

import scala.concurrent.duration._

class ServerPathTest extends FunSuite with Matchers with ScalatestRouteTest with BeforeAndAfterAll with MyJsonProtocol {


  def ws = WebServer.routes

  val wsClient1Room1 = WSProbe() // represents the client side of the web socket
  val wsClient2Room1 = WSProbe()
  val wsClient1Room2 = WSProbe()
  val wsClient2Room2 = WSProbe()
  val wsClient3Room2 = WSProbe()

  // not needed for now as currently creating new rooms for every webSocket
//  OpenRooms.createRoom() // Room 1
//  OpenRooms.createRoom() // Room 2

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

  test("adapter path returns adapter") {
    Get("/adapter") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("Javascript/lib/adapter.js").mkString
    }
  }

  test("room path returns room") {
    Get("/room") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("room.html").mkString
    }
  }

  test("test the webSocket path for user joining room") {
    WS("/webSocket/1?name=Alice", wsClient1Room1.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient1Room1.expectMessage(RoomID("roomID", 1, true).toJson.prettyPrint) // true as 1st user so is the caller

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

      wsClient2Room1.expectMessage(SendUser("user", "Alice").toJson.prettyPrint)

      wsClient2Room1.expectMessage(RoomID("roomID", 1, false).toJson.prettyPrint) // false as 2nd user so not the caller
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

      webSocketClient.expectMessage(RoomError("roomError").toJson.prettyPrint)

      webSocketClient.sendMessage("Test")
      webSocketClient.expectNoMessage(100.millis)
    }
  }

  test("client joins room 2") {
    WS("/webSocket/2?name=Sam", wsClient1Room2.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient1Room2.expectMessage(RoomID("roomID", 2, true).toJson.prettyPrint)

      assert(OpenRooms.openRooms.contains(2))

      wsClient1Room2.sendMessage("Test")
      wsClient1Room2.expectMessage("Test")

    }
  }

  test("second client joins room 2") {
    WS("/webSocket/2?name=Fred", wsClient2Room2.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient2Room2.expectMessage(SendUser("user", "Sam").toJson.prettyPrint)

      wsClient2Room2.expectMessage(RoomID("roomID", 2, false).toJson.prettyPrint)
      wsClient1Room2.expectNoMessage(100.millis)



      assert(OpenRooms.openRooms.size == 5) // for now as creating new room every time there is a new webSocket

      wsClient2Room2.sendMessage("Test")
      wsClient2Room2.expectMessage("Test")
      wsClient1Room2.expectMessage("Test")

    }
  }

  test("three clients in a room") {
    WS("/webSocket/2?name=Sarah", wsClient3Room2.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient3Room2.expectMessage(SendUser("user", "Sam").toJson.prettyPrint)
      wsClient3Room2.expectMessage(SendUser("user", "Fred").toJson.prettyPrint)

      wsClient3Room2.expectMessage(RoomID("roomID", 2, false).toJson.prettyPrint)
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

  test("hang up deletes the room") {
    WS("/webSocket/2?name=Sarah", wsClient3Room2.flow) ~> ws ~> check {

      val size = OpenRooms.openRooms.size

      wsClient3Room2.sendMessage("{\"tag\":\"hangUp\"}")

      Thread.sleep(200) // give a chance for the poison pills to be processed first

      assert(OpenRooms.openRooms.size == size - 1)
      assert(!OpenRooms.openRooms.contains(2))
    }
  }

}
