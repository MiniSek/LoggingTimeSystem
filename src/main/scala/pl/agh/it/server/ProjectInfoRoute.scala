package pl.agh.it.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import pl.agh.it.database.ProjectsDao
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import pl.agh.it.database.models.{Project, Task}
import pl.agh.it.server.TimeLoggingService.projectsDao
import pl.agh.it.server.config.MarshallingUnmarshallingConfiguration
import pl.agh.it.server.models.SortBy.SortByField
import pl.agh.it.server.models._

import java.time.LocalDateTime
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}
import concurrent.ExecutionContext.Implicits.global

/*
  Part of API routes responsible for project info retrieving.
  /info (with 'id' parameter) - project, its tasks and duration
  /info/all (with some parameters) - list of filtered projects with its tasks
*/
object ProjectInfoRoute extends MarshallingUnmarshallingConfiguration {
  def getRoute(projectsDao: ProjectsDao): Route = {
    pathPrefix("project") {
      concat(
        path("info") {
          get {
            parameter("id") { projectId =>
              onComplete(projectsDao.getProjectWithTasksAndDuration(projectId)) {
                case Success(projectInfo) =>
                  complete(StatusCodes.OK, ProjectTasksDurationResponse(projectInfo._1, projectInfo._2, projectInfo._3))
                case Failure(_: java.util.NoSuchElementException) =>
                  complete(StatusCodes.NotAcceptable, s"Project with id: $projectId isn't available");
                case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage);
              }
            }
          }
        },

        path("info" / "all") {
          get {
            parameters("page".as[Int].withDefault(1),
              "pageSize".as[Int].withDefault(10),
              "sortBy".optional,
              "reverse".optional,
              "id".repeated,
              "fromDate".optional,
              "toDate".optional,
              "byDeleted".optional) { (page, pageSize, sortBy, reverse, ids, fromDate, toDate, byDeleted) => {

              var returnError: String = ""
              if(page <= 0)
                returnError += s"Parameter page must be positive natural number" + "\n"
              if(pageSize <= 0)
                returnError += s"Parameter pageSize must be positive natural number" + "\n"

              var sortByEnum: SortByField = SortBy.None
              if (sortBy.isDefined) {
                if (SortBy.values.find(_.toString == sortBy.get).orNull == null)
                  returnError += s"Sorting by field ${sortBy.get} isn't possible [CreationTime/UpdateTime/None]" + "\n"
                else
                  sortByEnum = SortBy.withName(sortBy.get)
              }

              var reverseBoolean: Boolean = false
              if (reverse.isDefined) {
                if (reverse.get == "true")
                  reverseBoolean = true
                else if (reverse.get == "false")
                  reverseBoolean = false
                else
                  returnError += s"Parameter reverse ${reverse.get} is wrong [true/false]" + "\n"
              }

              var fromDateLDT: LocalDateTime = null
              var toDateLDT: LocalDateTime = null
              try {
                if (fromDate.isDefined)
                  fromDateLDT = LocalDateTime.parse(fromDate.get, formatter)
                if (toDate.isDefined)
                  toDateLDT = LocalDateTime.parse(toDate.get, formatter)
              } catch {
                case e: java.time.format.DateTimeParseException =>
                  returnError += e.getMessage + " format: [yyyy-MM-dd'T'HH:mm:ss.SSS]" + "\n"
              }

              var showByDeletion = "all"
              if (byDeleted.isDefined)
                if (byDeleted.get == "all" || byDeleted.get == "deleted" || byDeleted.get == "undeleted")
                  showByDeletion = byDeleted.get
                else
                  returnError += "Parameter byDeleted is wrong [all/deleted/undeleted]" + "\n"

              if(returnError == "") {
                if (sortByEnum == SortBy.CreationTime) { //when sorting by CreationTime get all projects
                  onComplete(projectsDao.getAllProjects) {
                    case Success(allProjects) =>
                      val selectedProjects = allProjects.sortBy(_.creationTimestamp) //and sort by this field
                      getSelectedProjectsRoute(applyFilters(selectedProjects, reverseBoolean, page, pageSize,
                        ids.toList, fromDateLDT, toDateLDT, showByDeletion))
                    case Failure(e) =>
                      complete(StatusCodes.InternalServerError, "Failure during accessing projects with message: " + e.getMessage)
                  }
                } else if (sortByEnum == SortBy.UpdateTime) { //when sorting by UpdateTime get all projects with its update times
                  onComplete(projectsDao.getAllProjectsWithUpdateTime) {
                    case Success(projectsWithUpdateTime) =>
                      val selectedProjects = projectsWithUpdateTime.sortBy(_._2).map(_._1) //sort by UpdateTime and map to only projects
                      getSelectedProjectsRoute(applyFilters(selectedProjects, reverseBoolean, page, pageSize,
                        ids.toList, fromDateLDT, toDateLDT, showByDeletion))
                    case Failure(e) =>
                      complete(StatusCodes.InternalServerError, "Failure during accessing projects with message: " + e.getMessage)
                  }
                } else {
                  onComplete(projectsDao.getAllProjects) { //when sorting isn't requested only get all projects
                    case Success(selectedProjects) =>
                      getSelectedProjectsRoute(applyFilters(selectedProjects, reverseBoolean, page, pageSize,
                        ids.toList, fromDateLDT, toDateLDT, showByDeletion))
                    case Failure(e) =>
                      complete(StatusCodes.InternalServerError, "Failure during accessing projects with message: " + e.getMessage)
                  }
                }
              } else
                  complete(StatusCodes.BadRequest, returnError.slice(0, returnError.length-1))
            }
            }
          }
        }
      )
    }
  }

  //method for sending response to info/all GET in every case (sort by CreationTime, UpdateTime and None)
  private def getSelectedProjectsRoute(allSelectedProjects: Future[AllProjectsResponse]): Route = {
    onComplete(allSelectedProjects) {
      case Success(selectedProjects) => complete(StatusCodes.OK, selectedProjects)
      case Failure(e) =>
        complete(StatusCodes.InternalServerError, "Failure during accessing tasks with message: " + e.getMessage)
    }
  }

  //method for applying filters to sequence of projects and collecting its tasks
  private def applyFilters(projectsSelected: Seq[Project], reverse: Boolean, page: Int, pageSize: Int,
                   idList: List[String], fromDate: LocalDateTime, toDate: LocalDateTime,
                   showByDeletion: String): Future[AllProjectsResponse] = {
    var projectsSelectedCopy: Seq[Project] = projectsSelected
    if(reverse)
      projectsSelectedCopy = projectsSelectedCopy.reverse
    projectsSelectedCopy = projectsSelectedCopy.slice((page - 1) * pageSize, page * pageSize)

    var filtered = projectsSelectedCopy
    if(idList.nonEmpty)
      filtered = filtered.filter(projectWithTasks => idList.contains(projectWithTasks.id))
    if(fromDate != null)
      filtered = filtered.filter(_.creationTimestamp.isAfter(fromDate))
    if(toDate != null)
      filtered = filtered.filter(_.creationTimestamp.isBefore(toDate))
    if(showByDeletion == "deleted")
      filtered = filtered.filter(_.deletedTimestamp.isDefined)
    if(showByDeletion == "undeleted")
      filtered = filtered.filter(_.deletedTimestamp.isEmpty)

    val listOfFutures = ListBuffer[Future[(Project, Seq[Task])]]()
    for(project <- filtered)
      listOfFutures += projectsDao.getProjectWithTasks(project.id)

    Future.sequence(listOfFutures.toSeq).map(_.flatten(Seq(_))).map { listOfProjectsWithTasks =>
      AllProjectsResponse(listOfProjectsWithTasks.collect {
        projectWithTasks => ProjectTasksResponse(projectWithTasks._1, projectWithTasks._2)
      })
    }
  }
}
