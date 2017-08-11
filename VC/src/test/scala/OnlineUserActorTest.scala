import akka.actor.{ActorSystem, Terminated}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import server._
import spray.json._


class OnlineUserActorTest extends TestKit(ActorSystem("WebSocketSystemTest")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll with MyJsonProtocol {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An onlineUser actor" must {

    val user = TestActorRef(new OnlineUser("test"))
    val client = TestProbe()
    val user2 = TestActorRef(new OnlineUser("test2"))
    val client2 = TestProbe()
    val watcher1 = TestProbe()
    val watcher2 = TestProbe()



    "add the new user to the onlineUsers map" in {
      UserManager.loggedIn += "test"
      user ! User("test", client.ref)
      assert(UserManager.onlineUsers.contains("test"))
      assert(UserManager.onlineUsers("test") == client.ref)
    }

    "send the client all online users" in {
      client.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", Set("test")).toJson.prettyPrint))
    }

    "when polled send the client all online users" in {
      user ! Poll
      client.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", Set("test")).toJson.prettyPrint))
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
  }

}
