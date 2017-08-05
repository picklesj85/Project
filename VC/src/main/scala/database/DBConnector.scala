package database

import java.sql.{Connection, DriverManager}


object DBConnector {

  def connect: Connection = {
    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://localhost/test"
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
    val resultSet = statement.executeQuery(s"select * from users where username = '$userName' and password = '$password'")
    resultSet.next()
  }
}

object test extends App {
  val connection = DBConnector.connect
  println(DBConnector.authenticate("james", "password", connection))
}

//val statement = connection.createStatement()
//val result = statement.executeQuery("select * from users")
//while (result.next()) {
//  val user = result.getString("username")
//  val pword = result.getString("password")
//  println(s"Username: $user Password: $pword")
//}