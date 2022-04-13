package pl.agh.it.server

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
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

class TasksIntegrationTest extends AnyWordSpec with Matchers with ScalatestRouteTest with DatabaseTestHelper
  with DatabaseSchema with MarshallingUnmarshallingConfiguration {

  val db = Database.forConfig("mysql")

  val projectsDao: ProjectsDao = new ProjectsDao(db)
  val tasksDao: TasksDao = new TasksDao(db)

  Await.ready(prepareDBForTaskIntegrationTests, Duration.Inf)

  val jwtUser001: String = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCIsIlVVSUQiOiJ1c2VyLXV1aWQtMDAxIn0.CGq0ri4JDkWSUWJadLo9EEHOG3pcnAx8cdMrKOxZFiA"

  "The service" should {
    val bodyTask1 =
      HttpEntity(ContentTypes.`application/json`,
        s"{\"projectId\":\"project-id-003\", \"startTimestamp\":\"2022-04-11T11:24:30.154\", \"durationInSeconds\":100}")
    val requestAddTask1: HttpRequest =
      HttpRequest(POST, uri = "/task", entity = bodyTask1).addHeader(RawHeader("Access-Token", jwtUser001))
    "return NotAcceptable code for POST request to add new task due to no such project" in {
      requestAddTask1 ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.NotAcceptable
      }
    }

    val bodyTask2 =
      HttpEntity(ContentTypes.`application/json`,
        s"{\"projectId\":\"project-id-001\", \"startTimestamp\":\"2022-04-11T11:24:30.154\", \"durationInSeconds\":100}")
    val requestAddTask2: HttpRequest =
      HttpRequest(POST, uri = "/task", entity = bodyTask2).addHeader(RawHeader("Access-Token", jwtUser001))
    "return OK code for POST request to add new task" in {
      requestAddTask2 ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Inserted new task with id: 1"
      }
    }

    val bodyTask3 = HttpEntity(ContentTypes.`application/json`, s"{\"id\":2, \"durationInSeconds\":200}")
    val requestAddTask3: HttpRequest =
      HttpRequest(PUT, uri = "/task", entity = bodyTask3).addHeader(RawHeader("Access-Token", jwtUser001))
    "return NotAcceptable code for PUT request to change task attributes due to no such task" in {
      requestAddTask3 ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.NotAcceptable
        responseAs[String] shouldEqual "Task with id: 2 isn't available"
      }
    }

    val bodyTask4 = HttpEntity(ContentTypes.`application/json`, s"{\"id\":1, \"durationInSeconds\":200}")
    val requestAddTask4: HttpRequest =
      HttpRequest(PUT, uri = "/task", entity = bodyTask4).addHeader(RawHeader("Access-Token", jwtUser001))
    "return OK code for PUT request to change task attributes" in {
      requestAddTask4 ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Changed task, its new id: 2"
      }
    }

    val bodyTask5 = HttpEntity(ContentTypes.`application/json`, s"{\"id\":2}")
    val requestAddTask5: HttpRequest =
      HttpRequest(DELETE, uri = "/task", entity = bodyTask5).addHeader(RawHeader("Access-Token", jwtUser001))
    "return OK code for DELETE request to delete task with id 1" in {
      requestAddTask5 ~> MainRoute.getMainRoute(projectsDao, tasksDao) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Task with id 2 deleted"
      }
    }
  }
}
