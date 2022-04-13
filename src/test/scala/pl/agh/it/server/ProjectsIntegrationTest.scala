package pl.agh.it.server

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.RawHeader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pl.agh.it.DatabaseTestHelper
import pl.agh.it.database.config.DatabaseSchema
import pl.agh.it.database.{ProjectsDao, TasksDao}
import pl.agh.it.server.config.MarshallingUnmarshallingConfiguration
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class ProjectsIntegrationTest extends AnyWordSpec with Matchers with ScalatestRouteTest with DatabaseTestHelper
  with DatabaseSchema with MarshallingUnmarshallingConfiguration {

  val db = Database.forConfig("mysql")

  val projectsDao: ProjectsDao = new ProjectsDao(db)
  val tasksDao: TasksDao = new TasksDao(db)

  Await.ready(createSchemaIfNotExists, Duration.Inf)
  Await.ready(clearDB, Duration.Inf)

  val jwtUser001: String = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCIsIlVVSUQiOiJ1c2VyLXV1aWQtMDAxIn0.CGq0ri4JDkWSUWJadLo9EEHOG3pcnAx8cdMrKOxZFiA"
  val jwtUser002: String = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCIsIlVVSUQiOiJ1c2VyLXV1aWQtMDAyIn0.x24PynKwFBgypw-IlCNUMFZyvgc5eZ1188kyfWJAUx0"

  "The service" should {
    val bodyProjectId001 = HttpEntity(ContentTypes.`application/json`, s"{\"id\":\"project-id-001\"}")
    val requestAddProject: HttpRequest =
      HttpRequest(POST, uri = "/project", entity = bodyProjectId001).addHeader(RawHeader("Access-Token", jwtUser001))
    "return OK code for POST request to add new project" in {
      requestAddProject ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Inserted new project with id: project-id-001"
      }
    }

    val requestNonUniqKey: HttpRequest =
      HttpRequest(POST, uri = "/project", entity = bodyProjectId001).addHeader(RawHeader("Access-Token", jwtUser001))
    "return Conflict code for POST request due to non uniq id" in {
      requestNonUniqKey ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.Conflict
      }
    }

    val bodyChangeId = HttpEntity(ContentTypes.`application/json`, s"{\"oldId\":\"project-id-001\", \"newId\":\"project-id-002\"}")
    val requestChangeId: HttpRequest =
      HttpRequest(PUT, uri = "/project", entity = bodyChangeId).addHeader(RawHeader("Access-Token", jwtUser001))
    "return OK code for PUT request to change project id" in {
      requestChangeId ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Project id project-id-001 changed to project-id-002"
      }
    }

    val requestDeleteProjectNotFound: HttpRequest =
      HttpRequest(DELETE, uri = "/project", entity = bodyProjectId001).addHeader(RawHeader("Access-Token", jwtUser001))
    "return NotAcceptable code for DELETE request to delete project that doesn't exist" in {
      requestDeleteProjectNotFound ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.NotAcceptable
      }
    }

    val bodyProjectId002 = HttpEntity(ContentTypes.`application/json`, s"{\"id\":\"project-id-002\"}")
    val requestDeleteProjectUser002: HttpRequest =
      HttpRequest(DELETE, uri = "/project", entity = bodyProjectId002).addHeader(RawHeader("Access-Token", jwtUser002))
    "return NotAcceptable code for DELETE request to delete project by another user" in {
      requestDeleteProjectUser002 ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.NotAcceptable
        responseAs[String] shouldEqual "Project with id project-id-002 isn't available and cannot be deleted"
      }
    }

    val requestDeleteProjectSuccess: HttpRequest =
      HttpRequest(DELETE, uri = "/project", entity = bodyProjectId002).addHeader(RawHeader("Access-Token", jwtUser001))
    "return OK code for DELETE request to delete project" in {
      requestDeleteProjectSuccess ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Project with id project-id-002 deleted"
      }
    }
  }
}
