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


  "A room moderator" must {

    "send the room number to new members" in {
      moderator ! User("Fred", client1.ref) // send a new client to the room
      client1.expectMsg(WrappedMessage(RoomID("roomID", 1, true).toJson.prettyPrint)) // true as 1st user so is caller
    }

    "forward on all messages" in {
      moderator ! WrappedMessage("hello")
      client1.expectMsg(WrappedMessage("hello"))
    }


    "send the existing names to the new member" in {
      moderator ! User("Sarah", client2.ref)
      client2.expectMsg(WrappedMessage(SendUser("user", "Fred").toJson.prettyPrint))
    }

    "send the new member the room number" in {
      client2.expectMsg(WrappedMessage(RoomID("roomID", 1, false).toJson.prettyPrint)) // false as 2nd user so not caller
    }

    "forward messages to all room members" in {
      moderator ! WrappedMessage("hi there")
      client1.expectMsg(WrappedMessage("hi there"))
      client2.expectMsg(WrappedMessage("hi there"))
    }

    "send the existing names to the third new member" in {
      moderator ! User("test", client3.ref)
      client3.expectMsg(WrappedMessage(SendUser("user", "Fred").toJson.prettyPrint))
      client3.expectMsg(WrappedMessage(SendUser("user", "Sarah").toJson.prettyPrint))
    }

    "send the third new member the room number" in {
      client3.expectMsg(WrappedMessage(RoomID("roomID", 1, false).toJson.prettyPrint))
    }

    "still forward messages to all room members" in {
      moderator ! WrappedMessage("three way")
      client1.expectMsg(WrappedMessage("three way"))
      client2.expectMsg(WrappedMessage("three way"))
      client3.expectMsg(WrappedMessage("three way"))
    }

    "when client tries to join no existent room, tell the client the room does not exist" in {
      val mod = TestActorRef(new RoomModerator(Room(Int.MinValue, system)))
      mod ! User("test", client3.ref)
      client3.expectMsg(WrappedMessage(RoomError("roomError").toJson.prettyPrint))
    }

    "when receives a hang up must Kill each member of the room" in {
      assert(OpenRooms.openRooms.size == 1)

      moderator ! WrappedMessage(hangUp)

      deathWatch1.expectMsgClass(classOf[Terminated])
      deathWatch2.expectMsgClass(classOf[Terminated])
      deathWatch3.expectMsgClass(classOf[Terminated])
    }

    "then delete the room" in {
      assert(OpenRooms.openRooms.isEmpty)
    }

    "finally kill himself" in {
      grimReaper.expectMsgClass(classOf[Terminated])
    }
  }

}
