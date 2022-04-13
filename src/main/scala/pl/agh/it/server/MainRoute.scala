package pl.agh.it.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, concat, headerValueByName}
import akka.http.scaladsl.server.Route
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import pl.agh.it.database.{ProjectsDao, TasksDao}

/*
  MainRoute class with root of paths. Authorization is processed here.
*/
object MainRoute {
  var uuid: String = ""
  //check whether user included JWT token, if it starts with 'Bearer ' and can be verified
  def getMainRoute(projectsDao: ProjectsDao, tasksDao: TasksDao): Route = {
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
}
