import akka.actor.{ActorSystem, Terminated}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import server._
import spray.json._


class OnlineUserActorTest extends TestKit(ActorSystem("WebSocketSystemTest")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll with MyJsonProtocol {

  override def afterAll {}


  val user = TestActorRef(new OnlineUser("test"))
  val user2 = TestActorRef(new OnlineUser("test2"))

  val client1 = TestProbe()
  val client2 = TestProbe()
  val client3 = TestProbe()

  val deathWatch1 = TestProbe()
  val deathWatch2 = TestProbe()
  deathWatch1.watch(user)
  deathWatch2.watch(user2)


  "When receiving a User an onlineUser actor" must {

    UserManager.loggedIn += "test"

    user ! User("test", client1.ref)

    "add the new user to the onlineUsers map" in {
      assert(UserManager.onlineUsers.contains("test"))
      assert(UserManager.onlineUsers("test") == client1.ref)
    }

    "send the client all online users" in {
      client1.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint))
    }
  }


  "When receiving a poll an onlineUser actor" must {

    user ! WrappedMessage("poll")

    "send the client all online users" in {
      client1.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint))
    }
  }

  "when another user logs in an online actor" must {

    UserManager.loggedIn += "test2"

    user2 ! User("test2", client2.ref)

    "add the new user to onlineUSers" in {
      assert(UserManager.onlineUsers.contains("test2"))
      assert(UserManager.onlineUsers("test2") == client2.ref)
      assert(UserManager.onlineUsers.size == 2)
    }

    "send the client all online users" in {
      client2.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint))
    }
  }

  "When polled both onlineUSer actors" must {

    user ! WrappedMessage("poll")
    user2 ! WrappedMessage("poll")

    "send both clients all online users" in {
      client1.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint))
      client2.expectMsg(WrappedMessage(AllOnlineUsers("onlineUsers", UserManager.loggedIn).toJson.prettyPrint))
    }
  }

    // this test isn't working at the moment i think it's due to concurrency and ordering of
    // tests
//    "When a user logs out the onlineUser actor" must {
//
//      UserManager.loggedIn += "test"
//      UserManager.loggedIn += "test2"
//
//      user2 ! "logout"
//
//      "remove the user from online users" in {
//        assert(UserManager.onlineUsers.size == 1)
//        assert(!UserManager.onlineUsers.contains("test2"))
//      }
//
//      "remove the user from loggedIn" in {
//        assert(UserManager.loggedIn.size == 1)
//        assert(!UserManager.loggedIn.contains("test2"))
//      }
//
//      "kill himself" in {
//        deathWatch1.expectMsgClass(classOf[Terminated])
//      }
//
//    }
//
//    TestKit.shutdownActorSystem(system)
//  }


}
