package pl.agh.it.database

import pl.agh.it.database.config.DatabaseSchema
import pl.agh.it.database.models.{DeleteProjectException, Project, Task}
import slick.jdbc.MySQLProfile.api._

import java.time.LocalDateTime
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ListBuffer


class ProjectsDao(db: Database) extends DatabaseSchema {
  //unblocking insertion of project to database
  def addProject(project: Project): Future[Int] = {
    db.run(projects += project)
  }

  //unblocking project id changing, if newId is already in database java.sql.SQLIntegrityConstraintViolationException throw
  def changeProjectId(authorUuid: String, oldId: String, newId: String): Future[Int] = {
    val query = projects.filter(_.authorUuid === authorUuid).filter(_.id === oldId).map(_.id).update(newId)
    db.run(query)
  }

  //unblocking soft deletion, if project won't be deleted its tasks also won't
  def softDelete(authorUuid: String, id: String): Future[Int] = {
    val maybeDate = Some(LocalDateTime.now())
    val deleteProjectQuery = projects.filter(_.authorUuid === authorUuid).filter(_.id === id)
      .filter(_.deletedTimestamp.isEmpty).map(_.deletedTimestamp).update(maybeDate)

    db.run(deleteProjectQuery).flatMap { deleted =>
        if(deleted == 1) {
          val deleteTasksQuery = tasks.filter(_.projectId === id).filter(_.deletedTimestamp.isEmpty)
            .map(_.deletedTimestamp).update(maybeDate)
          db.run(deleteTasksQuery)
        } else
          Future.failed(DeleteProjectException(s"Project with id $id isn't available and cannot be deleted"))
    }
  }

  //unblocking getter of all projects
  def getAllProjects: Future[Seq[Project]] = {
    db.run(projects.result)
  }

  //unblocking getter of exact project with its tasks, return failed Future if any failed
  def getProjectWithTasks(id: String): Future[(Project, Seq[Task])] = {
    val query = projects.filter(_.id === id)
    val projectTasksQuery = tasks.filter(_.projectId === id)
    for {
      project <- db.run(query.result.head)
      projectTasks <- db.run(projectTasksQuery.result)
    } yield (project, projectTasks)
  }

  //unblocking getter of exact project with its tasks and duration, return failed Future if any failed
  def getProjectWithTasksAndDuration(id: String): Future[(Project, Seq[Task], Long)] = {
    val query = projects.filter(_.id === id)
    val projectTasksQuery = tasks.filter(_.projectId === id)
    for {
      project <- db.run(query.result.head)
      projectTasks <- db.run(projectTasksQuery.result)
    } yield (project, projectTasks, projectTasks.map(_.durationInSeconds).sum)
  }

  //unblocking getter of all tuples consisting of project and its update time
  def getAllProjectsWithUpdateTime: Future[Seq[(Project, LocalDateTime)]] = {
    db.run(projects.result).flatMap { allProjects =>
        val result = ListBuffer[Future[(Project, LocalDateTime)]]()
        for (project <- allProjects)
          result += getUpdateTimestamp(project)
      Future.sequence(result.toSeq).map(_.flatten(Seq(_)))
    }
  }

  //unblocking getter of update time of project
  private def getUpdateTimestamp(project: Project): Future[(Project, LocalDateTime)] = {
    db.run(tasks.filter(_.projectId === project.id).map(_.creationTimestamp).max.result).map(maybeTimestamp =>
      if(maybeTimestamp.isEmpty)
        (project, project.creationTimestamp)
      else
        (project, maybeTimestamp.get)
    )
  }
}
