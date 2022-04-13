package pl.agh.it.database

import org.scalatest.flatspec.AsyncFlatSpec
import pl.agh.it.DatabaseTestHelper
import pl.agh.it.database.config.DatabaseSchema
import pl.agh.it.database.models.Task
import pl.agh.it.server.config.LDTFormatterConfiguration
import slick.jdbc.JdbcBackend.Database

import java.time.LocalDateTime
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class TasksDaoTest extends AsyncFlatSpec with DatabaseSchema  with DatabaseTestHelper with LDTFormatterConfiguration {
  val db = Database.forConfig("mysql")
  Await.ready(createSchemaIfNotExists, Duration.Inf)

  val tasksDao: TasksDao = new TasksDao(db)
  Await.ready(prepareDBForTasksTests, Duration.Inf)

  val dateTimeEx1: LocalDateTime = LocalDateTime.parse("2022-04-09T10:50:51.908", formatter)

  behavior of "getUserTask"
  it should "eventually return user task" in {
    val future: Future[Task] = tasksDao.getUserTask(1, "user-uuid-001")
    future map { task => assert(task.authorUuid == "user-uuid-001")}
  }
  it should "eventually call exception due to lack of task with such id" in {
    recoverToSucceededIf[java.util.NoSuchElementException] {
      tasksDao.getUserTask(5, "user-uuid-001")
    }
  }
  it should "eventually call exception due to lack of task of this user with such id" in {
    recoverToSucceededIf[java.util.NoSuchElementException] {
      tasksDao.getUserTask(1, "user-uuid-002")
    }
  }

  behavior of "getAllUserTasks"
  it should "eventually return sequence of projects" in {
    val future: Future[Seq[Task]] = tasksDao.getAllUserNotDeletedTasks("user-uuid-002")
    future map { allTasks => assert(allTasks.size == 2)}
  }
  it should "eventually return empty sequence of projects" in {
    val future: Future[Seq[Task]] = tasksDao.getAllUserNotDeletedTasks("user-uuid-003")
    future map { allTasks => assert(allTasks.isEmpty)}
  }

  behavior of "softDelete"
  it should "eventually soft delete one task" in {
    val future: Future[Int] = tasksDao.softDelete(1,"user-uuid-001")
    future map { deleted => assert(deleted == 1)}
  }
  it should "eventually not soft delete one task due to authority" in {
    val future: Future[Int] = tasksDao.softDelete(2, "user-uuid-001")
    future map { deleted => assert(deleted == 0)}
  }
  it should "eventually not soft delete one task due to its earlier removal" in {
    val future: Future[Int] = tasksDao.softDelete(1, "user-uuid-001")
    future map { deleted => assert(deleted == 0)}
  }

  behavior of "canTaskBeAddedBlocking"
  it should "not be possible to add task" in {
    val dateTimeEx2 = LocalDateTime.parse("2022-04-09T10:55:21.908", formatter)
    val newTask = Task(authorUuid="user-uuid-002", projectId="project-id-001", startTimestamp=dateTimeEx2, durationInSeconds=25)
    val future: Future[Boolean] = tasksDao.canTaskBeAdded(newTask)
    future map { deleted => assert(!deleted)}
  }
  it should "be possible to add task" in {
    val dateTimeEx2 = LocalDateTime.parse("2022-04-09T10:55:21.908", formatter)
    val newTask = Task(authorUuid="user-uuid-002", projectId="project-id-001", startTimestamp=dateTimeEx2, durationInSeconds=25)
    val future: Future[Boolean] = tasksDao.canTaskBeAdded(newTask, Seq(2))
    future map { deleted => assert(deleted)}
  }
}
