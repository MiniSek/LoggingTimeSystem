package pl.agh.it.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import pl.agh.it.database.ProjectsDao
import pl.agh.it.database.models.{DeleteProjectException, Project}
import pl.agh.it.server.models._

import scala.util.{Failure, Success}

/*
  Part of API routes responsible for projects creating (post), changing its id (put) and deleting (delete).
*/
object ProjectRoute extends MarshallingUnmarshallingConfiguration {
  def getRoute(projectsDao: ProjectsDao, uuid: String): Route = {
    path("project") {
      concat(
        post {
          entity(as[ProjectIdRequest]) {
            request =>
              onComplete(projectsDao.addProject(Project(request.id, uuid))) {
                case Success(_) => complete(StatusCodes.OK, s"Inserted new project with id: ${request.id}")
                case Failure(e: java.sql.SQLIntegrityConstraintViolationException) => complete(StatusCodes.Conflict, e.getMessage)
                case Failure(e) =>
                  complete(StatusCodes.InternalServerError, "Failure during project addition with message: " + e.getMessage)
              }
          }
        },

        put {
          entity(as[ChangeProjectIdRequest]) {
            request =>
              onComplete(projectsDao.changeProjectId(uuid, request.oldId, request.newId)) {
                case Success(changed) =>
                  if (changed == 1)
                    complete(StatusCodes.OK, s"Project id ${request.oldId} changed to ${request.newId}")
                  else
                    complete(StatusCodes.NotAcceptable, s"Project with id ${request.oldId} isn't available and cannot be changed")
                case Failure(e: java.sql.SQLIntegrityConstraintViolationException) => complete(StatusCodes.Conflict, e.getMessage)
                case Failure(e) =>
                  complete(StatusCodes.InternalServerError, "Failure during project changing with message: " + e.getMessage)
              }
          }
        },

        delete {
          entity(as[ProjectIdRequest]) {
            request =>
              onComplete(projectsDao.softDelete(uuid, request.id)) {
                case Success(_) => complete(StatusCodes.OK, s"Project with id ${request.id} deleted")
                case Failure(e: DeleteProjectException) => complete(StatusCodes.NotAcceptable, e.getMessage)
                case Failure(e) => complete(StatusCodes.InternalServerError,
                  s"Project ${request.id} deleted\nFailure occur when deleting its tasks with message: " + e.getMessage)
              }
          }
        },
      )
    }
  }
}
