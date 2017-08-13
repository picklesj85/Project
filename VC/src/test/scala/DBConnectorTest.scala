import database.DBConnector
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class DBConnectorTest extends FunSuite with BeforeAndAfterAll {

  val connection = DBConnector.connect

  val DBUserCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM USERS")
    result.next()
  }

  val DBContactCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM CONTACTS")
    result.next()
  }

  val DBPendingCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM PENDING")
    result.next()
  }

  override def afterAll() = {

    connection.createStatement().executeUpdate("DELETE FROM CONTACTS " +
                                               "WHERE CONTACT1 = 'testContact1'" +
                                               "OR CONTACT2 = 'testContact1'")

    connection.createStatement().executeUpdate("DELETE FROM PENDING WHERE REQUESTEE = 'testContact1'")

    val endUserSize = {
      val statement = connection.createStatement()
      val result = statement.executeQuery("SELECT COUNT(*) FROM USERS")
      result.next()
    }

    val endContactCount = {
      val statement = connection.createStatement()
      val result = statement.executeQuery("SELECT COUNT(*) FROM CONTACTS")
      result.next()
    }

    val endPendingCount = {
      val statement = connection.createStatement()
      val result = statement.executeQuery("SELECT COUNT(*) FROM PENDING")
      result.next()
    }

    assert(DBUserCount == endUserSize) // make sure we haven't accidentally added to the database during testing
    assert(DBContactCount == endContactCount)
    assert(DBPendingCount == endPendingCount)

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
    assert(DBConnector.createUser("testUser", "test", connection))
  }

  test("create new user but name already exists") {
    assert(!DBConnector.createUser("testUser", "test", connection))
  }

  test("delete user") {
    assert(DBConnector.deleteUser("testUser", "test", connection))
  }

  test("delete non-existing user") {
    assert(!DBConnector.deleteUser("mike", "pword", connection))
  }

  test("delete user wrong password") {
    assert(!DBConnector.deleteUser("james", "wrongPassword", connection))
  }

  test("new contacts writes to DB") {
    assert(DBConnector.newContact("testContact1", "testContact2", connection))
    assert(DBConnector.newContact("testContact3", "testContact1", connection))
    assert(DBConnector.newContact("testContact1", "testContact4", connection))
    assert(DBConnector.newContact("testContact5", "testContact1", connection))
  }

  test("get contacts") {
    val contactSet = Set("testContact2", "testContact5", "testContact4", "testContact3")
    assert(DBConnector.getMyContacts("testContact1", connection) == contactSet)
  }

  test("new pending contact writes to DB") {
    assert(DBConnector.newPendingContact("testContact2", "testContact1", connection))
    assert(DBConnector.newPendingContact("testContact3", "testContact1", connection))
    assert(DBConnector.newPendingContact("testContact4", "testContact1", connection))
    assert(DBConnector.newPendingContact("testContact5", "testContact1", connection))
  }

  test("get pending contacts") {
    val contactSet = Set("testContact2", "testContact4", "testContact5", "testContact3")
    assert(DBConnector.getPendingContacts("testContact1", connection) == contactSet)
  }

  test("search contacts") {

    // add lots of j names to search
    assert(DBConnector.createUser("john", "test", connection))
    assert(DBConnector.createUser("jim", "test", connection))
    assert(DBConnector.createUser("joan", "test", connection))
    assert(DBConnector.createUser("jenny", "test", connection))

    val resultSet = Set("john", "jim", "jenny", "joan", "james")

    assert(DBConnector.searchContacts("j", connection) == resultSet)

  }

  test("search again") {
    val resultSet2 = Set("joan", "john")

    assert(DBConnector.searchContacts("jo", connection) == resultSet2)

    // delete al the j names added
    assert(DBConnector.deleteUser("john", "test", connection))
    assert(DBConnector.deleteUser("jim", "test", connection))
    assert(DBConnector.deleteUser("joan", "test", connection))
    assert(DBConnector.deleteUser("jenny", "test", connection))
  }

  test("search full name") {
    assert(DBConnector.searchContacts("james", connection) == Set("james"))
  }
}

