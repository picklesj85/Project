import akka.actor.{Actor, ActorSystem, Props, Terminated}
import akka.testkit.{ImplicitSender, TestActor, TestActorRef, TestActors, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers, WordSpecLike}
import server._
import spray.json._


class RoomModeratorActorTest extends TestKit(ActorSystem("WebSocketSystemTest")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll with MyJsonProtocol {



  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  // create a test room moderator
  val moderator = TestActorRef(new RoomModerator(OpenRooms.createRoom()))


  // create some test clients
  val client1 = TestProbe()
  val client2 = TestProbe()
  val client3 = TestProbe()

  // create some 'watchers' to watch the test probe actors to monitor behaviour
  val deathWatch1 = TestProbe() // to make sure the other probes die when they are supposed to!
  val deathWatch2 = TestProbe()
  val deathWatch3 = TestProbe()
  val grimReaper = TestProbe() // to make sure the moderator kills himself!

  // set the watchers to watch
  grimReaper.watch(moderator)
  deathWatch1.watch(client1.ref)
  deathWatch2.watch(client2.ref)
  deathWatch3.watch(client3.ref)

  val hangUp = "{\"tag\":\"hangUp\"}" // the string that a hangup message will come as


  moderator ! User("Fred", client1.ref) // send a new client to the room


  "A room moderator" must {

    "send the room number to new members" in {
      client1.expectMsg(WrappedMessage(RoomID("roomID", 1, true).toJson.prettyPrint)) // true as 1st user so is caller
    }

    "forward on all messages" in {
      moderator ! WrappedMessage("hello")
      client1.expectMsg(WrappedMessage("hello"))
    }
  }


  "When another client joins the same room a moderator" must {

    moderator ! User("Sarah", client2.ref)

    "send the existing names to the new member" in {
      client2.expectMsg(WrappedMessage(SendUser("user", "Fred").toJson.prettyPrint))
    }

    "send the new member the room number" in {
      client2.expectMsg(WrappedMessage(RoomID("roomID", 1, false).toJson.prettyPrint)) // false as 2nd user so not caller
    }

    "forward messages to all room members" in {
      moderator ! WrappedMessage("hi there")
      client1.expectMsg(WrappedMessage("hi there"))
      client2.expectMsg(WrappedMessage("hello"))
      client2.expectMsg(WrappedMessage("hi there"))
    }

  }

  "when a third client joins a room a moderator" must {
    moderator ! User("test", client3.ref)

    "send the existing names to the new member" in {
      client3.expectMsg(WrappedMessage(SendUser("user", "Fred").toJson.prettyPrint))
      client3.expectMsg(WrappedMessage(SendUser("user", "Sarah").toJson.prettyPrint))
    }

    "send the new member the room number" in {
      client3.expectMsg(WrappedMessage(RoomID("roomID", 1, false).toJson.prettyPrint))
    }

    "forward messages to all room members" in {
      moderator ! WrappedMessage("three way")
      client1.expectMsg(WrappedMessage("three way"))
      client2.expectMsg(WrappedMessage("three way"))
      client3.expectMsg(WrappedMessage("hello"))
      client3.expectMsg(WrappedMessage("hi there"))
      client3.expectMsg(WrappedMessage("three way"))
    }
  }

  "When a client tries to join a non existent room a moderator" must {

    val mod = TestActorRef(new RoomModerator(Room(Int.MinValue, system)))

    "tell the client the room does not exist" in {
      mod ! User("test", client3.ref)
      client3.expectMsg(WrappedMessage(RoomError("roomError").toJson.prettyPrint))
    }
  }

  "When a hangUp message is received a room moderator" must {

    "Kill each member of the room" in {
      assert(OpenRooms.openRooms.size == 1)

      moderator ! WrappedMessage(hangUp)

      deathWatch1.expectMsgClass(classOf[Terminated])
      deathWatch2.expectMsgClass(classOf[Terminated])
      deathWatch3.expectMsgClass(classOf[Terminated])
    }

    "Delete the room" in {
      assert(OpenRooms.openRooms.isEmpty)
    }

    "Kill himself" in {
      grimReaper.expectMsgClass(classOf[Terminated])
    }
  }

}
