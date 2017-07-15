import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers, WordSpecLike}

class WebSocketSystemTest extends TestKit(ActorSystem("WebSocketSystemTest")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val room1 = new Room(1, system)
  val room1Moderator = room1.roomModerator
  val user1 = User("James", system.actorOf(Props(new TestActors.BlackholeActor)))
  //val testActor = system.actorOf(Props(new TestActors.BlackholeActor))

  "A room moderator" must {

    "add each user sent to the room" in {
      room1Moderator ! MyMessage("hello")
      //user1.actorRef.expectMsg(MyMessage("hello"))

    }
  }
}
