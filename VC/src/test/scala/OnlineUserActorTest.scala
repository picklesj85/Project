import akka.actor.{ActorSystem, Terminated}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import database.DBConnector
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import server._
import spray.json._

import scala.concurrent.duration._

class OnlineUserActorTest extends TestKit(ActorSystem("WebSocketSystemTest")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll with MyJsonProtocol {

  val connection = DBConnector.connect

  val DBContactCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM CONTACTS")
    result.next()
  }

  val DBPendingCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM PENDING")
    result.next()
  }

  override def beforeAll(): Unit = {
    // create some test entries in the database
    // test1 profile
    DBConnector.newContact("james", "test1", connection)
    DBConnector.newContact("sarah", "test1", connection)

    // test2 profile
    DBConnector.newPendingContact("mike", "test2", connection)
    DBConnector.newPendingContact("terry", "test2", connection)
    DBConnector.newContact("dave", "test2", connection)

    // test 3 profile
    DBConnector.newPendingContact("dan", "test3", connection)
    DBConnector.newContact("pete", "test3", connection)
    DBConnector.newContact("test3", "tim", connection)
    DBConnector.newContact("al", "test3", connection)
    DBConnector.newContact("test3", "fay", connection)


    connection.close()
  }
  override def afterAll: Unit = {
    val statement = connection.createStatement()

    statement.executeUpdate("DELETE FROM CONTACT WHERE CONTACT1 = 'test1'  OR CONTACT2 = 'test1'")
    statement.executeUpdate("DELETE FROM CONTACT WHERE CONTACT1 = 'test2'  OR CONTACT2 = 'test2'")
    statement.executeUpdate("DELETE FROM CONTACT WHERE CONTACT1 = 'test3'  OR CONTACT2 = 'test3'")

    statement.executeUpdate("DELETE FROM PENDING WHERE REQUESTEE = 'test1'")
    statement.executeUpdate("DELETE FROM PENDING WHERE REQUESTEE = 'test2'")
    statement.executeUpdate("DELETE FROM PENDING WHERE REQUESTEE = 'test3'")

    val endContactCount = {
      val statement = connection.createStatement()
      val result = statement.executeQuery("SELECT COUNT(*) FROM CONTACTS")
      result.next()
    }

    val endPendingCount = {
      val statement = connection.createStatement()
      val result = statement.executeQuery("SELECT COUNT(*) FROM PENDING")
      result.next()
    }

    assert(DBContactCount == endContactCount)
    assert(DBPendingCount == endPendingCount)
    connection.close()
    TestKit.shutdownActorSystem(system)
  }

  val user = TestActorRef(new OnlineUser("test1"))
  val client = TestProbe()
  val user2 = TestActorRef(new OnlineUser("test2"))
  val client2 = TestProbe()
  val user3 = TestActorRef(new OnlineUser("test3"))
  val client3 = TestProbe()
  val user4 = TestActorRef(new OnlineUser("test4"))
  val client4 = TestProbe()
  val watcher1 = TestProbe()
  val watcher2 = TestProbe()
  val watcher3 = TestProbe()

  "An onlineUser actor" must {

    "kill himself if the user isn't authenticated" in {
      watcher3.watch(user4)
      user4 ! User("notAuth", client4.ref)
      watcher3.expectMsgClass(classOf[Terminated])
    }

    "add the new user to the onlineUsers map" in {
      UserManager.loggedIn += "test1"
      UserManager.loggedIn += "james"
      user ! User("test1", client.ref)
      assert(UserManager.onlineUsers.contains("test1"))
      assert(UserManager.onlineUsers("test1") == client.ref)
    }

    "send the client all contacts" in {
      client.expectMsg(WrappedMessage(OnlineContacts("online", Set("james")).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(OfflineContacts("offline", Set("sarah")).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(PendingContacts("pending", Set()).toJson.prettyPrint))
    }

    "start the poll loop after 3 seconds" in {
      Thread.sleep(3000)
      client.expectMsg(WrappedMessage(OnlineContacts("online", Set("james")).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(OfflineContacts("offline", Set("sarah")).toJson.prettyPrint))
    }

    "when polled send the client all online users" in {
      UserManager.loggedIn += "sarah"
      user ! Poll
      client.expectMsg(WrappedMessage(OnlineContacts("online", Set("james", "sarah")).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(OfflineContacts("offline", Set()).toJson.prettyPrint))
    }

    "create room when requested" in {
      user ! WrappedMessage("createRoom")
      assert(OpenRooms.openRooms.size == 1)
      client.expectMsg(WrappedMessage(RoomNumber("roomNumber", OpenRooms.openRooms.head._1).toJson.prettyPrint))
    }

    "add another new user to onlineUsers" in {
      UserManager.loggedIn += "test2"
      user2 ! User("test2", client2.ref)
      assert(UserManager.onlineUsers.contains("test2"))
      assert(UserManager.onlineUsers("test2") == client2.ref)
      assert(UserManager.onlineUsers.size == 2)
    }

    "and send the client all online users" in {
      client2.expectMsg(WrappedMessage(OnlineContacts("online", Set()).toJson.prettyPrint))
      client2.expectMsg(WrappedMessage(OfflineContacts("offline", Set("dave")).toJson.prettyPrint))
      client2.expectMsg(WrappedMessage(PendingContacts("pending", Set("mike", "terry")).toJson.prettyPrint))
    }

    "when both polled send both clients all online users" in {
      UserManager.loggedIn += "dave"
      user ! Poll
      user2 ! Poll
      client.expectMsg(WrappedMessage(OnlineContacts("online", Set("james", "sarah")).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(OfflineContacts("offline", Set()).toJson.prettyPrint))
      client2.expectMsg(WrappedMessage(OnlineContacts("online", Set("dave")).toJson.prettyPrint))
      client2.expectMsg(WrappedMessage(OfflineContacts("offline", Set()).toJson.prettyPrint))
    }

    "create another room when requested" in {
      user2 ! WrappedMessage("createRoom")
      assert(OpenRooms.openRooms.size == 2)
      client2.expectMsg(WrappedMessage(RoomNumber("roomNumber", 2).toJson.prettyPrint))
    }

    "will send call details to both users" in {
      user ! WrappedMessage("calltest2")
      assert(OpenRooms.openRooms.size == 3)
      client2.expectMsg(WrappedMessage(ReceiveCall("receiveCall", "test1", 3).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(SendCall("sendCall", 3).toJson.prettyPrint))
    }

    "will send call details to the other user" in {
      user2 ! WrappedMessage("calltest")
      assert(OpenRooms.openRooms.size == 4)
      client.expectMsg(WrappedMessage(ReceiveCall("receiveCall", "test2", 4).toJson.prettyPrint))
      client2.expectMsg(WrappedMessage(SendCall("sendCall", 4).toJson.prettyPrint))
    }

    "will send an accepted when receive accepted" in {
      user ! WrappedMessage("acceptedtest2")
      client2.expectMsg(WrappedMessage(Accepted("accepted").toJson.prettyPrint))
    }

    "will send an accepted the other way" in {
      user2 ! WrappedMessage("acceptedtest")
      client.expectMsg(WrappedMessage(Accepted("accepted").toJson.prettyPrint))
    }

    "will forward a reject on" in {
      user ! WrappedMessage("rejectedtest2")
      client2.expectMsg(WrappedMessage(Rejected("rejected").toJson.prettyPrint))
    }

    "will forward a reject on the other way" in {
      user2 ! WrappedMessage("rejectedtest")
      client.expectMsg(WrappedMessage(Rejected("rejected").toJson.prettyPrint))
    }

    "checking a third user" in {
      UserManager.loggedIn += "test1"
      UserManager.loggedIn += "pete"
      UserManager.loggedIn += "tim"
      UserManager.loggedIn += "fay"
      user3 ! User("test3", client.ref)
      assert(UserManager.onlineUsers.contains("test3"))
      assert(UserManager.onlineUsers("test3") == client.ref)
    }

    "still send the client all contacts" in {
      client.expectMsg(WrappedMessage(OnlineContacts("online", Set("pete", "tim", "fay")).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(OfflineContacts("offline", Set("al")).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(PendingContacts("pending", Set("dan")).toJson.prettyPrint))
    }

    "still when polled send the client all online users" in {
      UserManager.loggedIn += "al"
      UserManager.loggedIn -= "fay"
      UserManager.loggedIn -= "pete"
      user ! Poll
      client.expectMsg(WrappedMessage(OnlineContacts("online", Set("tim", "al")).toJson.prettyPrint))
      client.expectMsg(WrappedMessage(OfflineContacts("offline", Set("fay", "pete")).toJson.prettyPrint))
    }

    "when user logs out remove the user from online users" in {
      watcher2.watch(user2)
      user2 ! WrappedMessage("logout")
      assert(UserManager.onlineUsers.size == 1)
      assert(!UserManager.onlineUsers.contains("test2"))
    }

    "and remove the user from loggedIn" in {
      assert(UserManager.loggedIn.size == 1)
      assert(!UserManager.loggedIn.contains("test2"))
    }

    "finally kill himself" in {
      watcher2.expectMsgClass(classOf[Terminated])
    }

    "when another user logs out remove the user from online users" in {
      watcher1.watch(user)
      user ! WrappedMessage("logout")
      assert(UserManager.onlineUsers.isEmpty)
    }

    "then remove the user from loggedIn" in {
      assert(UserManager.loggedIn.isEmpty)
    }

    "then finally kill himself" in {
      watcher1.expectMsgClass(classOf[Terminated])
    }

  }

}
