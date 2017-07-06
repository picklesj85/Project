
import org.scalatest._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import Directives._
import scala.io.Source

class ServerPathTest extends FunSuite with Matchers with ScalatestRouteTest {

  val ws = WebServer.routes

  test("root path returns index.html") {
    Get() ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("index.html").mkString
    }
  }

  test("js path returns main.js") {
    Get("/js") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("Javascript/main.js").mkString
    }
  }

  test("css path returns main.css") {
    Get("/css") ~> ws ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual Source.fromResource("CSS/main.css").mkString
    }
  }
}
