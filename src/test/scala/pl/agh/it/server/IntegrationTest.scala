package pl.agh.it.server

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.RawHeader
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pl.agh.it.database.configuration.{DatabaseHelper, DatabaseSchema}
import pl.agh.it.database.{ProjectsDao, TasksDao}
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class IntegrationTest extends AnyWordSpec with Matchers with ScalatestRouteTest with DatabaseHelper
  with DatabaseSchema with MarshallingUnmarshallingConfiguration {

  val db = Database.forConfig("mysql")
  val jwt: String = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCIsIlVVSUQiOiJtaWtvbGFqLWthcmR5cy11dWlkIn0.KdVinhLwUgfJLAlu0yU_4EJKFWPbz_wMPuJa0-qZvW4"

  val algorithm = Algorithm.HMAC256("secret")
  val verifier = JWT.require(algorithm).withIssuer("auth0").build

  var uuid: String = ""

  Await.ready(createSchemaIfNotExists, Duration.Inf)
  Await.ready(clearDB, Duration.Inf)

  "The service" should {
    val requestWithoutHeader: HttpRequest = HttpRequest(POST, uri = "/projects")
    "return rejection for request without \'Access-Token header\'" in {
      requestWithoutHeader ~> TimeLoggingService.getMainRoute ~> check {
        rejection shouldEqual MissingHeaderRejection("Access-Token")
      }
    }

    val requestWithHeaderWithoutBearer: HttpRequest =
      HttpRequest(POST, uri = "/projects").addHeader(RawHeader("Access-Token", jwt.replace("Bearer ", "")))
    "return Unauthorized code for request with jwt token without \'Bearer \'" in {
      requestWithHeaderWithoutBearer ~> TimeLoggingService.getMainRoute ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[String] shouldEqual "Access-Token doesn't start with \'Bearer \'"
      }
    }

    val requestWithWrongToken: HttpRequest =
      HttpRequest(POST, uri = "/projects").addHeader(RawHeader("Access-Token", jwt + "a"))
    "return Unauthorized code for request with wrong jwt token" in {
      requestWithWrongToken ~> TimeLoggingService.getMainRoute ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[String] shouldEqual "Cannot verify JWT token"
      }
    }


    val r = HttpEntity(ContentTypes.`application/json`, s"{\"id\":\"project-id-001\"}")

    val requestSuccess: HttpRequest =
      HttpRequest(POST, uri = "/project", entity = r).addHeader(RawHeader("Access-Token", jwt))
    "return OK code for POST request to add new project" in {
      requestSuccess ~> TimeLoggingService.getMainRoute ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
