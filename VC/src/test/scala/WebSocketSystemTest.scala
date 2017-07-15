import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class WebSocketSystemTest extends FunSuite with BeforeAndAfterAll {

  implicit val system = ActorSystem()

  override def afterAll {
    system.terminate()
  }

  /**
    * OpenRooms tests
    */

  test("count increases when adding new room") {
    OpenRooms.createRoom()
    assert(OpenRooms.IDcount == 1)
  }
  test("count increases again") {
    OpenRooms.createRoom()
    assert(OpenRooms.IDcount == 2)
  }
  test("rooms are being added to the map") {
    assert(OpenRooms.openRooms.size == 2)
    assert(OpenRooms.openRooms.keySet == Set(1, 2))
  }
}
