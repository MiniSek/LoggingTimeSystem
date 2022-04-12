package pl.agh.it.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import pl.agh.it.database.{ProjectsDao, TasksDao}
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.Duration
import scala.io.StdIn
import pl.agh.it.database.configuration.{DatabaseHelper, DatabaseSchema}

/*
  Main app to run service.
*/
object TimeLoggingService extends App with DatabaseSchema with DatabaseHelper {
  val db = Database.forConfig("mysql")
  Await.ready(createSchemaIfNotExists, Duration.Inf)
  println("---Service ready---")

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "my-system")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val projectsDao: ProjectsDao = new ProjectsDao(db)
  val tasksDao: TasksDao = new TasksDao(db)

  var uuid: String = ""
  def getMainRoute = {  //check whether user included JWT token, if it starts with 'Bearer ' and can be verified
    concat(
      headerValueByName("Access-Token") { rawToken => {
        if(rawToken.startsWith("Bearer ")) {
          val token: String = rawToken.replace("Bearer ", "")
          try {
            val algorithm = Algorithm.HMAC256("secret")
            val verifier = JWT.require(algorithm).withIssuer("auth0").build

            verifier.verify(token)
            uuid = JWT.decode(token).getClaim("UUID").asString()
            if(uuid != null && uuid != "")
              concat(ProjectRoute.getRoute(projectsDao, uuid),
                TaskRoute.getRoute(tasksDao, uuid),
                ProjectInfoRoute.getRoute(projectsDao))
            else
              complete(StatusCodes.Unauthorized, "JWT token doesn't have user UUID claim")
          } catch {
            case _: JWTVerificationException =>
              complete(StatusCodes.Unauthorized, "Cannot verify JWT token")
          }
        } else {
          complete(StatusCodes.Unauthorized, "Access-Token doesn't start with \'Bearer \'")
        }
      }
      }
    )
  }

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(getMainRoute)
  println("---Print enter to shut down service---")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
}

