import database.DBConnector
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class DBConnectorTest extends FunSuite with BeforeAndAfterAll {

  val connection = DBConnector.connect

  val DBUserCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM USERS")
    result.next()
  }

  override def afterAll() = {

    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM USERS")
    val endSize = result.next()

    assert(DBUserCount == endSize) // make sure we haven't accidentally added to the database during testing

    connection.close()

  }

  test("authenticate with correct credentials") {
    assert(DBConnector.authenticate("james", "password", connection))
  }

  test("authenticate with incorrect username") {
    assert(!DBConnector.authenticate("wrongUser", "password", connection))
  }

  test("authenticate with incorrect password") {
    assert(!DBConnector.authenticate("james", "wrongPassword", connection))
  }

  test("name available with non-available name") {
    assert(!DBConnector.nameAvailable("james", connection))
  }

  test("name available with available name") {
    assert(DBConnector.nameAvailable("mike", connection))
  }

  test("create new user") {
    assert(DBConnector.createUser("test", "test", connection))
  }

  test("create new user but name already exists") {
    assert(!DBConnector.createUser("test", "test", connection))
  }

  test("delete user") {
    assert(DBConnector.deleteUser("test", "test", connection))
  }

  test("delete non-existing user") {
    assert(!DBConnector.deleteUser("mike", "pword", connection))
  }

  test("delete user wrong password") {
    assert(!DBConnector.deleteUser("james", "wrongPassword", connection))
  }

}

