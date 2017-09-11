import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import server._

class OpenRoomsTest extends FunSuite with BeforeAndAfterAll {

  implicit val system = ActorSystem()

  override def afterAll {
    system.terminate()
  }



  test("generates random positive ID and deletes room") {
    for (i <- 1 to 1000) {
      val room = OpenRooms.createRoom()
      assert(room.roomID >= 0)
      assert(room.roomID < 1000000)
    }

  }

  test("rooms are being added to the map") {
    assert(OpenRooms.openRooms.size == 1000)
  }

  test("delete rooms") {
    for (room <- OpenRooms.openRooms) {
      OpenRooms.deleteRoom(room._1)
    }
    assert(OpenRooms.openRooms.isEmpty)
  }


  test("find non existent room returns the roomError room") {
    val room = OpenRooms.findRoom(1)
    assert(room.roomID == Int.MinValue)
  }

  test("find room returns correct room") {
    OpenRooms.openRooms += 1000 -> Room(1000, system)

    val room = OpenRooms.findRoom(1000)

    assert(OpenRooms.openRooms.size == 1)
    assert(room.roomID == 1000)
  }
}
