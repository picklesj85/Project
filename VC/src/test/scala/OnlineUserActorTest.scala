import akka.actor.{ActorSystem, Terminated}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import server._
import spray.json._
import scala.concurrent.duration._

class OnlineUserActorTest extends TestKit(ActorSystem("WebSocketSystemTest")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll with MyJsonProtocol {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val user = TestActorRef(new OnlineUser("test"))
  val client = TestProbe()
  val user2 = TestActorRef(new OnlineUser("test2"))
  val client2 = TestProbe()
  val user3 = TestActorRef(new OnlineUser("test3"))
  val client3 = TestProbe()
  val watcher1 = TestProbe()
  val watcher2 = TestProbe()
  val watcher3 = TestProbe()

  "An onlineUser actor" must {

    "kill himself if the user isn't authenticated" in {
      watcher3.watch(user3)
      user3 ! User("notAuth", client3.ref)
      watcher3.expectMsgClass(classOf[Terminated])
    }

    "add the new user to the onlineUsers map" in {
      UserManager.loggedIn += "test"
      user ! User("test", client.ref)
      assert(UserManager.onlineUsers.contains("test"))
      assert(UserManager.onlineUsers("test") == client.ref)
    }

    "send the client all online users" in {
      client.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", Set("test")).toJson.prettyPrint))
    }

    "start the poll loop after 3 seconds" in {
      Thread.sleep(3000)
      client.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", Set("test")).toJson.prettyPrint))
    }

    "when polled send the client all online users" in {
      user ! Poll
      client.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", Set("test")).toJson.prettyPrint))
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
      client2.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", Set("test", "test2")).toJson.prettyPrint))
    }

    "when both polled send both clients all online users" in {
      user ! Poll
      user2 ! Poll
      client.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", Set("test", "test2")).toJson.prettyPrint))
      client2.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", Set("test", "test2")).toJson.prettyPrint))
    }

    "create another room when requested" in {
      user2 ! WrappedMessage("createRoom")
      assert(OpenRooms.openRooms.size == 2)
      client2.expectMsg(WrappedMessage(RoomNumber("roomNumber", 2).toJson.prettyPrint))
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
