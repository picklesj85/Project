import database.DBConnector
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class DBConnectorTest extends FunSuite with BeforeAndAfterAll {

  val connection = DBConnector.connect

  var DBUserCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM USERS")
    if (result.next()) result.getInt(1) else Int.MinValue
  }

  val DBContactCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM CONTACTS")
    if (result.next()) result.getInt(1) else Int.MinValue
  }

  val DBPendingCount = {
    val statement = connection.createStatement()
    val result = statement.executeQuery("SELECT COUNT(*) FROM PENDING")
    if (result.next()) result.getInt(1) else Int.MinValue
  }

  override def afterAll() = {

    connection.createStatement().executeUpdate("DELETE FROM CONTACTS " +
                                               "WHERE CONTACT1 = 'testContact1'" +
                                               "OR CONTACT2 = 'testContact1'")

    connection.createStatement().executeUpdate("DELETE FROM PENDING " +
                                               "WHERE REQUESTEE = 'testContact1'" +
                                               "OR REQUESTEE = 'testContact6'")

    val endUserSize = {
      val statement = connection.createStatement()
      val result = statement.executeQuery("SELECT COUNT(*) FROM USERS")
      if (result.next()) result.getInt(1) else Int.MinValue
    }

    val endContactCount = {
      val statement = connection.createStatement()
      val result = statement.executeQuery("SELECT COUNT(*) FROM CONTACTS")
      if (result.next()) result.getInt(1) else Int.MinValue
    }

    val endPendingCount = {
      val statement = connection.createStatement()
      val result = statement.executeQuery("SELECT COUNT(*) FROM PENDING")
      if (result.next()) result.getInt(1) else Int.MinValue
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

  test("new contact deletes from pending") {
    val statement = connection.createStatement()
    assert(DBConnector.newPendingContact("testContact1", "testContact6", connection))
    val result1 = statement.executeQuery("SELECT COUNT(REQUESTEE) FROM PENDING WHERE REQUESTEE = 'testContact6'")
    if (result1.next()) assert(result1.getInt(1) == 1)
    assert(DBConnector.newContact("testContact1", "testContact6", connection))
    val result2 = statement.executeQuery("SELECT COUNT(REQUESTEE) FROM PENDING WHERE REQUESTEE = 'testContact6'")
    if (result2.next()) assert(result2.getInt(1) == 0)
  }

  test("get contacts") {
    val contactSet = Set("testContact2", "testContact5", "testContact4", "testContact3", "testContact6")
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

  test("delete pending contacts") {
    assert(DBConnector.newPendingContact("james", "mark", connection))
    assert(DBConnector.deletePending("james", "mark", connection))
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

