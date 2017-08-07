import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import server._

class OpenRoomsTest extends FunSuite with BeforeAndAfterAll {

  implicit val system = ActorSystem()

  override def afterAll {
    system.terminate()
  }



  test("count increases when adding new room") {
    val room = OpenRooms.createRoom()
    assert(OpenRooms.IDcount == 1)
    assert(room.roomID == 1)
  }

  test("count increases again") {
    val room = OpenRooms.createRoom()
    assert(OpenRooms.IDcount == 2)
    assert(room.roomID == 2)
  }

  test("rooms are being added to the map") {
    assert(OpenRooms.openRooms.size == 2)
    assert(OpenRooms.openRooms.keySet == Set(1, 2))
  }

  test("delete rooms") {
    OpenRooms.deleteRoom(1)
    assert(OpenRooms.openRooms.size == 1)
  }

  test("delete another room") {
    OpenRooms.deleteRoom(2)
    assert(OpenRooms.openRooms.isEmpty)
  }

  test("find non existent room returns the roomError room") {
    val room = OpenRooms.findRoom(1)
    assert(room.roomID == Int.MinValue)
  }

  test("find room returns correct room") {
    OpenRooms.createRoom()

    val room = OpenRooms.findRoom(3)

    assert(OpenRooms.openRooms.size == 1)
    assert(room.roomID == 3)
  }
}
