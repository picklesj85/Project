
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{FormData, StatusCodes}
import org.scalatest._
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.ActorMaterializer
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.Location
import akka.testkit.TestProbe

import scala.io.Source
import akka.util.ByteString
import database.DBConnector
import server.WebServer.getFromResource
import server._

import scala.concurrent.duration._
import scala.util.matching.Regex

class ServerPathTest extends FunSuite with Matchers with ScalatestRouteTest with BeforeAndAfterAll with MyJsonProtocol {

  val connection = DBConnector.connect

  override def beforeAll { // write test entry to database
    DBConnector.createUser("testUser", "testPassword", connection)
    // testContact1
    DBConnector.newPendingContact("test", "testUser", connection)
    DBConnector.newContact("anotherTest", "testUser", connection)
    //testContact2
    DBConnector.newPendingContact("dave", "testUser2", connection)
    DBConnector.newPendingContact("sarah", "testUser2", connection)
    DBConnector.newContact("james", "testUser2", connection)
    DBConnector.newContact("testUser2", "alice", connection)
    // test contact3
    DBConnector.newContact("james", "testUser3", connection)
    DBConnector.newContact("sarah", "testUser3", connection)

    OpenRooms.openRooms += 1 -> Room(1, system)

  }

  override def afterAll: Unit = { // delete test entry
    DBConnector.deleteUser("testUser", "testPassword", connection)
    DBConnector.deleteUser("newUser", "newPassword", connection)
    connection.createStatement().executeUpdate("DELETE FROM CONTACTS WHERE CONTACT2 = 'testUser'")
    connection.createStatement().executeUpdate("DELETE FROM PENDING WHERE REQUESTEE = 'testUser'")
    connection.createStatement().executeUpdate("DELETE FROM CONTACTS WHERE CONTACT2 = 'testUser2' OR CONTACT1 = 'testUser2'")
    connection.createStatement().executeUpdate("DELETE FROM PENDING WHERE REQUESTEE = 'testUser2'")
    connection.createStatement().executeUpdate("DELETE FROM CONTACTS WHERE CONTACT2 = 'testUser3'")
    connection.close()
  }

  def ws = WebServer.routes

  val wsClient1Room1 = WSProbe() // represents the client side of the web socket
  val wsClient2Room1 = WSProbe()
  val wsClient1Room2 = WSProbe()
  val wsClient2Room2 = WSProbe()
  val wsClient3Room2 = WSProbe()

  val wsClient1 = WSProbe()
  val wsClient2 = WSProbe()
  val wsClient3 = WSProbe()


  // not needed for now as currently creating new rooms for every webSocket
//  OpenRooms.createRoom() // Room 1
//  OpenRooms.createRoom() // Room 2

  test("get on root path returns index.html") {
    Get() ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("index.html").mkString
    }
  }

  test("post on root path returns room number") {
    Post() ~> ws ~> check {
      status shouldBe OK
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

  test("home path returns home when logged in") {
    UserManager.login("test")
    Get("/home?user=test") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("home.html").mkString
    }
  }

  test("home path does not return home if not logged in") {
    UserManager.logout("test")
    Get("/home?user=test") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual "You must be logged in to view this page."
    }
  }

  test("loginError path returns loginError") {
    Get("/loginError") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("loginError.html").mkString
    }
  }

  test("get createAccount path returns createAccount") {
    Get("/createAccount") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("createAccount.html").mkString
    }
  }

  test("get homeJs path returns home.js") {
    Get("/homeJs") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("Javascript/home.js").mkString
    }
  }

  test("userExists path returns usernameExists") {
    Get("/userExists") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("usernameExists.html").mkString
    }
  }

  test("incorrect credentials wrong password") {
    Post("/credentials", FormData("username" -> "testUser", "password" -> "wrongPassword")) ~> ws ~> check {
      status shouldBe SeeOther
      response.header[Location] should be (Some(Location("loginError")))
    }
  }

  test("incorrect credentials wrong username") {
    Post("/credentials", FormData("username" -> "testser", "password" -> "testPassword")) ~> ws ~> check {
      status shouldBe SeeOther
      response.header[Location] should be (Some(Location("loginError")))
    }
  }

  test("correct credentials") {
    Post("/credentials", FormData("username" -> "testUser", "password" -> "testPassword")) ~> ws ~> check {
      status shouldBe SeeOther
      response.header[Location] should be (Some(Location("home?user=testUser")))

      assert(UserManager.loggedIn.size == 1)
    }
  }

  test("create account existing account") {
    Post("/createAccount", FormData("username" -> "testUser", "password" -> "testPassword")) ~> ws ~> check {
      status shouldBe SeeOther
      response.header[Location] should be (Some(Location("userExists")))

      assert(UserManager.loggedIn.size == 1)
    }
  }

  test("create account new account") {
    Post("/createAccount", FormData("username" -> "newUser", "password" -> "newPassword")) ~> ws ~> check {
      status shouldBe SeeOther
      response.header[Location] should be (Some(Location("home?user=newUser")))

      assert(UserManager.loggedIn.size == 2)
    }
  }


  test("test the webSocket path for user joining room") {
    WS("/webSocket/1?name=Sarah", wsClient1Room1.flow) ~> ws ~> check {

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

      wsClient2Room1.expectMessage(SendUser("user", "Sarah").toJson.prettyPrint)

      wsClient2Room1.expectMessage(RoomID("roomID", 1, false).toJson.prettyPrint) // false as 2nd user so not the caller
      wsClient1Room1.expectNoMessage(100.millis)

      wsClient2Room1.sendMessage("hello")
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

  OpenRooms.openRooms += 2 -> Room(2, system)

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



      assert(OpenRooms.openRooms.size == 3)

      wsClient2Room2.sendMessage("Test")
      wsClient2Room2.expectMessage("Test")
      wsClient1Room2.expectMessage("Test")


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

  test("test the loggedIn webSocket path one logged in user") {

    UserManager.loggedIn += "testUser"

    WS("/loggedIn?user=testUser", wsClient1.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient1.expectMessage(PendingContacts("pending", Set("test")).toJson.prettyPrint)
      wsClient1.expectMessage(OnlineContacts("online", Set()).toJson.prettyPrint)
      wsClient1.expectMessage(OfflineContacts("offline", Set("anotherTest")).toJson.prettyPrint)

      assert(UserManager.onlineUsers.size == 1)
      assert(UserManager.onlineUsers.head._1 == "testuser")

      Thread.sleep(3000) // wait 3 seconds to check poll loop working
      wsClient1.expectMessage(OnlineContacts("online", Set()).toJson.prettyPrint)
      wsClient1.expectMessage(OfflineContacts("offline", Set("anotherTest")).toJson.prettyPrint)
    }
  }

  test("test the loggedIn webSocket path two logged in users") {

    UserManager.loggedIn += "testuser2"

    WS("/loggedIn?user=testUser2", wsClient2.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient2.expectMessage(PendingContacts("pending", Set("dave", "sarah")).toJson.prettyPrint)
      wsClient2.expectMessage(OnlineContacts("online", Set()).toJson.prettyPrint)
      wsClient2.expectMessage(OfflineContacts("offline", Set("james", "alice")).toJson.prettyPrint)

      assert(UserManager.onlineUsers.size == 2)
      assert(UserManager.onlineUsers.contains("testuser2"))

      Thread.sleep(3000)
      wsClient2.expectMessage(OnlineContacts("online", Set()).toJson.prettyPrint)
      wsClient2.expectMessage(OfflineContacts("offline", Set("james", "alice")).toJson.prettyPrint)

    }
  }

  test("test the loggedIn webSocket path three logged in users") {

    UserManager.loggedIn += "testuser3"
    UserManager.loggedIn += "james"

    WS("/loggedIn?user=testUser3", wsClient3.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      wsClient3.expectMessage(PendingContacts("pending", Set()).toJson.prettyPrint)
      wsClient3.expectMessage(OnlineContacts("online", Set("james")).toJson.prettyPrint)
      wsClient3.expectMessage(OfflineContacts("offline", Set("sarah")).toJson.prettyPrint)

      assert(UserManager.onlineUsers.size == 3)
      assert(UserManager.onlineUsers.contains("testuser3"))

      UserManager.loggedIn -= "james"
      UserManager.loggedIn += "sarah"

      Thread.sleep(3000)
      wsClient3.expectMessage(OnlineContacts("online", Set("sarah")).toJson.prettyPrint)
      wsClient3.expectMessage(OfflineContacts("offline", Set("james")).toJson.prettyPrint)

    }
  }

  test("test the loggedIn webSocket with a logout") {


    WS("/loggedIn?user=testUser", wsClient1.flow) ~> ws ~> check {

      isWebSocketUpgrade shouldEqual true

      assert(UserManager.onlineUsers.size == 3)
      assert(UserManager.onlineUsers.contains("testuser3"))


      wsClient3.sendMessage("logout")
      Thread.sleep(1000)
      assert(UserManager.onlineUsers.size == 2)
      assert(!UserManager.onlineUsers.contains("testuser3"))

      wsClient2.sendMessage("logout")
      Thread.sleep(1000)
      assert(UserManager.onlineUsers.size == 1)
      assert(!UserManager.onlineUsers.contains("testuser2"))

      wsClient1.sendMessage("logout")
      Thread.sleep(1000)
      assert(UserManager.onlineUsers.isEmpty)

    }
  }

}
