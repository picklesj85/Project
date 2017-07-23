import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActor, TestActorRef, TestActors, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers, WordSpecLike}

class ActorTest extends TestKit(ActorSystem("WebSocketSystemTest")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {



  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val moderator = TestActorRef(new RoomModerator(Room(1, system)))

  val probe1 = TestProbe()
  val probe2 = TestProbe()
  val probe3 = TestProbe()

  moderator ! User("Fred", probe1.ref)

  "A room moderator" must {

    "send the room number to new members" in {
      probe1.expectMsg(WrappedMessage("1"))
    }

    "send a welcome message to new members" in {
      probe1.expectMsg(WrappedMessage("Welcome to Room 1!"))
    }

    "forward on all messages" in {
      moderator ! WrappedMessage("hello")
      probe1.expectMsg(WrappedMessage("hello"))
    }

  }

  "When another client joins the same room a moderator" must {

    moderator ! User("Sarah", probe2.ref)

    "send the existing names to the new member" in {
      probe2.expectMsg(WrappedMessage("userFred"))
    }

    "send the new member the room number and welcome message" in {
      probe2.expectMsg(WrappedMessage("1"))
      probe2.expectMsg(WrappedMessage("Welcome to Room 1!"))
    }

    "forward messages to all room members" in {
      moderator ! WrappedMessage("hi there")
      probe1.expectMsg(WrappedMessage("hi there"))
      probe2.expectMsg(WrappedMessage("hello"))
      probe2.expectMsg(WrappedMessage("hi there"))
    }

  }

  "when a third client joins a room a moderator" must {
    moderator ! User("test", probe3.ref)

    "send the existing names to the new member" in {
      probe3.expectMsg(WrappedMessage("userFred"))
      probe3.expectMsg(WrappedMessage("userSarah"))
    }

    "send the new member the room number and welcome message" in {
      probe3.expectMsg(WrappedMessage("1"))
      probe3.expectMsg(WrappedMessage("Welcome to Room 1!"))
    }

    "forward messages to all room members" in {
      moderator ! WrappedMessage("three way")
      probe1.expectMsg(WrappedMessage("three way"))
      probe2.expectMsg(WrappedMessage("three way"))
      probe3.expectMsg(WrappedMessage("hello"))
      probe3.expectMsg(WrappedMessage("hi there"))
      probe3.expectMsg(WrappedMessage("three way"))
    }
  }

  "When a client tries to join a non existent room a moderator" must {

    val mod = TestActorRef(new RoomModerator(Room(Int.MaxValue, system)))

    "tell the client the room does not exist" in {
      mod ! User("test", probe3.ref)
      probe3.expectMsg(WrappedMessage("The Room ID entered does not exist."))
    }
  }


}
