package database

import java.sql.{Connection, DriverManager}


object DBConnector {

  def connect: Connection = {
    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://localhost/VC"
    val username = "root"
    val password = "root"

    var connection: Connection = null

    try {
      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)
    } catch {
      case e => e.printStackTrace
    }
    connection
  }

  def authenticate(userName: String, password: String, connection: Connection): Boolean = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT * FROM USERS WHERE USERNAME = '$userName' AND PASSWORD = '$password'")
    resultSet.next()
  }

  def nameAvailable(name: String, connection: Connection): Boolean = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT * FROM USERS WHERE USERNAME = '$name'")
    !resultSet.next() // if empty name is available
  }

  def createUser(name: String, password: String, connection: Connection): Boolean = {
    if (nameAvailable(name, connection)) {
      val statement = connection.createStatement()
      return statement.executeUpdate(s"INSERT INTO USERS VALUES('$name', '$password')") == 1
    }
    false
  }

  def deleteUser(name: String, password: String, connection: Connection): Boolean = {
    if (nameAvailable(name, connection)) {
      false // user does not exist
    } else {
      val statement = connection.createStatement()
      statement.executeUpdate(s"DELETE FROM USERS WHERE USERNAME = '$name' AND PASSWORD = '$password'") == 1 // check both for security
    }
  }

  def getMyContacts(name: String, connection: Connection): Set[String] = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT CONTACT1 FROM CONTACTS WHERE CONTACT2 = '$name' " +
                                           s"UNION " +
                                           s"SELECT CONTACT2 FROM CONTACTS WHERE CONTACT1 = '$name'")
    new Iterator[String] {
      def hasNext = resultSet.next()
      def next() = resultSet.getString(1)
    }.toSet
  }

  def getPendingContacts(name: String, connection: Connection): Set[String] = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT REQUESTOR FROM PENDING WHERE REQUESTEE = '$name'")

    new Iterator[String] {
      def hasNext = resultSet.next()
      def next() = resultSet.getString(1)
    }.toSet
  }

  def newContact(contact1: String, contact2: String, connection: Connection): Boolean = {
    val statement = connection.createStatement()
    deletePending(contact1, contact2, connection)
    statement.executeUpdate(s"INSERT INTO CONTACTS VALUES('$contact1', '$contact2')") == 1
  }

  def newPendingContact(requestor: String, requestee: String, connection: Connection): Boolean = {
    val statement = connection.createStatement()
    statement.executeUpdate(s"INSERT INTO PENDING VALUES('$requestor', '$requestee')") == 1
  }

  def searchContacts(search: String, connection: Connection): Set[String] = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT USERNAME FROM USERS WHERE USERNAME LIKE '$search%'")

    new Iterator[String] {
      def hasNext = resultSet.next()
      def next() = resultSet.getString(1)
    }.toSet
  }

  def deletePending(requestor: String, requestee: String, connection: Connection): Boolean = {
    val statement = connection.createStatement()
    return statement.executeUpdate(s"DELETE FROM PENDING WHERE REQUESTOR = '$requestor' AND REQUESTEE = '$requestee'") == 1
  }
}



//val statement = connection.createStatement()
//val result = statement.executeQuery("select * from users")
//while (result.next()) {
//  val user = result.getString("username")
//  val pword = result.getString("password")
//  println(s"Username: $user Password: $pword")
//}