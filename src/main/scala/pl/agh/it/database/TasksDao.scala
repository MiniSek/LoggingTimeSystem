package pl.agh.it.database

import pl.agh.it.database.configuration.DatabaseSchema
import pl.agh.it.database.models.{NoSuchProjectException, Task}
import slick.jdbc.MySQLProfile.api._

import java.time.LocalDateTime
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global


class TasksDao(db: Database) extends DatabaseSchema {
  //unblocking getter of task, which cannot be deleted and is available only for its author
  def getUserTask(id: Int, userUuid: String): Future[Task] = {
    val query = tasks.filter(_.id === id).filter(_.authorUuid === userUuid).filter(_.deletedTimestamp.isEmpty)
    db.run(query.result.head)
  }

  //unblocking soft deletion of task; task to delete cannot be already deleted and this operation is available for task author
  def softDelete(id: Int, userUuid: String): Future[Int] = {
    val query = tasks.filter(_.id === id).filter(_.authorUuid === userUuid).filter(_.deletedTimestamp.isEmpty)
      .map(_.deletedTimestamp).update(Some(LocalDateTime.now()))
    db.run(query)
  }

  //unblocking getter of user all undeleted tasks
  def getAllUserNotDeletedTasks(userUuid: String): Future[Seq[Task]] = {
    val query = tasks.filter(_.authorUuid === userUuid).filter(_.deletedTimestamp.isEmpty)
    db.run(query.result)
  }

  //unblocking task addition, it inserts task regardless of its interceptions with other ones; returns id of inserted task
  def unsafeAddTask(task: Task): Future[Task] = {
    val insertQuery = tasks returning tasks.map(_.id) into ((_, newId) => task.copy(id = Some(newId)))
    val query = insertQuery += task
    db.run(query)
  }

  //unblocking method for checking whether task can be added, if task project doesn't exist or task intercepts task cannot be added
  def canTaskBeAdded(task: Task, invalidTasksIds: Seq[Int] = Seq()): Future[Boolean] = {
    db.run(projects.filter(_.id === task.projectId).filter(_.deletedTimestamp.isEmpty).result).flatMap { project =>
      if(project.isEmpty)
        Future.failed(NoSuchProjectException(s"Project with id ${task.projectId} isn't available"))
      else
        getAllUserNotDeletedTasks(task.authorUuid).map { userTasks =>
          checkInterception(task.startTimestamp, task.startTimestamp.plusSeconds(task.durationInSeconds),
            userTasks.filter(task => {
              if(task.id.isDefined)
                !invalidTasksIds.contains(task.id.get)
              else
                false
            }))
        }
    }
  }

  //method for checking interceptions; start of task and end of task (start of task + duration) is considered
  private def checkInterception(start: LocalDateTime, end: LocalDateTime, tasks: Seq[Task]): Boolean = {
    var tStart, tEnd: LocalDateTime = LocalDateTime.now()
    tasks.foreach(task => {
      tStart = task.startTimestamp
      tEnd = task.startTimestamp.plusSeconds(task.durationInSeconds)
      if ((start.isAfter(tStart) && start.isBefore(tEnd)) || (end.isAfter(tStart) && end.isBefore(tEnd)) ||
        (end.isEqual(tEnd) && start.isEqual(tStart)))
        return false
    })
    true
  }
}
