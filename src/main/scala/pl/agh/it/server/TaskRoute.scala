package pl.agh.it.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, concat, delete, entity, onComplete, path, post, put}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import pl.agh.it.database.TasksDao
import pl.agh.it.database.models.{NoSuchProjectException, Task}
import pl.agh.it.server.config.{LDTFormatterConfiguration, MarshallingUnmarshallingConfiguration}
import pl.agh.it.server.models.{AddTaskRequest, ChangeTaskAttributesRequest, DeleteTaskRequest}

import concurrent.ExecutionContext.Implicits.global
import java.time.LocalDateTime
import scala.util.{Failure, Success}

/*
  Part of API routes responsible for task creating (post), changing (put) and deleting (delete).
*/
object TaskRoute extends MarshallingUnmarshallingConfiguration with LDTFormatterConfiguration {
  def getRoute(tasksDao: TasksDao, uuid: String): Route = {
    path("task") {
      concat(
        post {
          entity(as[AddTaskRequest]) {
            request => {
              try {
                val startTimestamp: LocalDateTime = LocalDateTime.parse(request.startTimestamp, formatter)
                val newTask = Task(authorUuid = uuid, projectId = request.projectId, startTimestamp = startTimestamp,
                  durationInSeconds = request.durationInSeconds, volume = request.volume, description = request.description)

                onComplete(tasksDao.canTaskBeAdded(newTask)) {
                  case Success(canBeAdded) =>
                    if (canBeAdded)
                      onComplete(tasksDao.unsafeAddTask(newTask)) {
                        case Success(task) =>
                          complete(StatusCodes.OK, s"Inserted new task with id: ${task.id.get}")
                        case Failure(e) =>
                          complete(StatusCodes.InternalServerError, "Failure during adding task with message: " + e.getMessage)
                      }
                    else
                      complete(StatusCodes.Conflict, "Task intercept with another one of this user")
                  case Failure(e: NoSuchProjectException) => complete(StatusCodes.NotAcceptable, e.getMessage)
                  case Failure(e) =>
                    complete(StatusCodes.InternalServerError, "Failure during checking interceptions with message: " + e.getMessage)
                }
              } catch {
                case e: java.time.format.DateTimeParseException => complete(StatusCodes.BadRequest, e.getMessage)
              }
            }
          }
        },

        delete {
          entity(as[DeleteTaskRequest]) {
            request => {
              onComplete(tasksDao.softDelete(request.id, uuid)) {
                case Success(changed) =>
                  if (changed == 1)
                    complete(StatusCodes.OK, s"Task with id ${request.id} deleted")
                  else
                    complete(StatusCodes.NotAcceptable, s"Task with id ${request.id} isn't available and cannot be deleted")
                case Failure(e) =>
                  complete(StatusCodes.InternalServerError, "Failure during task deleting with message: " + e.getMessage)
              }
            }
          }
        },

        put {
          entity(as[ChangeTaskAttributesRequest]) {
            request => {
              onComplete(tasksDao.getUserTask(request.id, uuid)) {
                case Success(task) =>
                  try {
                    val newStartTimestamp =
                      if (request.startTimestamp.isDefined) LocalDateTime.parse(request.startTimestamp.get, formatter)
                      else task.startTimestamp
                    val newDurationInSeconds =
                      if (request.durationInSeconds.isDefined) request.durationInSeconds.get else task.durationInSeconds
                    val newVolume = if (request.volume.isDefined) Some(request.volume.get) else task.volume
                    val newDescription = if (request.description.isDefined) Some(request.description.get)
                    else task.description

                    val newTask = Task(authorUuid = uuid, projectId = task.projectId, startTimestamp = newStartTimestamp,
                      durationInSeconds = newDurationInSeconds, volume = newVolume, description = newDescription,
                      creationTimestamp = task.creationTimestamp)

                    onComplete(tasksDao.canTaskBeAdded(newTask, Seq(task.id.get))) {
                      case Success(canBeAdded) =>
                        if (canBeAdded) {
                          onComplete(for {
                            addition <- tasksDao.unsafeAddTask(newTask)
                            deletion <- tasksDao.softDelete(task.id.get, uuid)
                          } yield (addition, deletion)) {
                            case Success((task, _)) => complete(StatusCodes.OK, s"Changed task, its new id: ${task.id.get}")
                            case Failure(e) =>
                              complete(StatusCodes.InternalServerError, "Failure during changing task with message: " + e.getMessage)
                          }
                        } else
                          complete(StatusCodes.Conflict, "Task intercept with another one of this user")
                      case Failure(e: NoSuchProjectException) => complete(StatusCodes.NotAcceptable, e.getMessage)
                      case Failure(e) =>
                        complete(StatusCodes.InternalServerError, "Failure during checking interceptions with message: " + e.getMessage)
                    }
                  } catch {
                    case e: java.time.format.DateTimeParseException => complete(StatusCodes.BadRequest, e.getMessage)
                  }
                case Failure(_: java.util.NoSuchElementException) =>
                  complete(StatusCodes.NotAcceptable, s"Task with id: ${request.id} isn't available");
                case Failure(e) => complete(StatusCodes.NotAcceptable, "Failure during accessing task with message: " + e.getMessage)
              }
            }
          }
        }
      )
    }
  }
}
