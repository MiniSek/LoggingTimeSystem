package pl.agh.it.server

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.MissingHeaderRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pl.agh.it.DatabaseTestHelper
import pl.agh.it.database.{ProjectsDao, TasksDao}
import pl.agh.it.database.config.DatabaseSchema
import pl.agh.it.server.config.MarshallingUnmarshallingConfiguration
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class AuthIntegrationTest extends AnyWordSpec with Matchers with ScalatestRouteTest with DatabaseTestHelper
  with DatabaseSchema with MarshallingUnmarshallingConfiguration {

  val db = Database.forConfig("mysql")

  val projectsDao: ProjectsDao = new ProjectsDao(db)
  val tasksDao: TasksDao = new TasksDao(db)

  Await.ready(createSchemaIfNotExists, Duration.Inf)
  Await.ready(clearDB, Duration.Inf)

  val jwtUser001: String = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCIsIlVVSUQiOiJ1c2VyLXV1aWQtMDAxIn0.CGq0ri4JDkWSUWJadLo9EEHOG3pcnAx8cdMrKOxZFiA"

  "The service" should {
    val requestWithoutHeader: HttpRequest = HttpRequest(POST, uri = "/projects")
    "return rejection for request without \'Access-Token header\'" in {
      requestWithoutHeader ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        rejection shouldEqual MissingHeaderRejection("Access-Token")
      }
    }

    val requestWithHeaderWithoutBearer: HttpRequest =
      HttpRequest(POST, uri = "/projects").addHeader(RawHeader("Access-Token", jwtUser001.replace("Bearer ", "")))
    "return Unauthorized code for request with jwt token without \'Bearer \'" in {
      requestWithHeaderWithoutBearer ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[String] shouldEqual "Access-Token doesn't start with \'Bearer \'"
      }
    }

    val requestWithWrongToken: HttpRequest =
      HttpRequest(POST, uri = "/projects").addHeader(RawHeader("Access-Token", jwtUser001 + "a"))
    "return Unauthorized code for request with wrong jwt token" in {
      requestWithWrongToken ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[String] shouldEqual "Cannot verify JWT token"
      }
    }
  }
}
