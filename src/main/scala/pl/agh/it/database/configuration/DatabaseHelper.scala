package pl.agh.it.database.configuration

import pl.agh.it.database.models.{Project, Task}
import pl.agh.it.server.LDTFormatterConfiguration
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import java.time.LocalDateTime
import scala.concurrent.{Await, Future}
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait DatabaseHelper extends LDTFormatterConfiguration {
  self: DatabaseSchema =>

  def db: Database

  def createSchemaIfNotExists: Future[Unit] = {
    db.run(MTable.getTables).flatMap(tables =>
      if (tables.isEmpty)
        db.run(allSchemas.create)
      else
        Future.successful()
    )
  }

  def clearDB: Future[Unit] = {
    db.run(DBIO.seq(projects.delete, tasks.delete))
  }

  def prepareDBForProjectTests: Future[Unit] = {
    val timestamp = LocalDateTime.parse("2022-04-10T13:20:30.094", formatter)
    db.run(DBIO.seq(
      projects.delete, tasks.delete,

      projects ++= Seq(
        Project("project-id-001", "user-uuid-001"),
        Project("project-id-002", "user-uuid-002"),
        Project("project-id-003", "user-uuid-002")
      ),

      tasks ++= Seq(
        Task(authorUuid = "user-uuid-001", projectId = "project-id-003", startTimestamp = timestamp, durationInSeconds = 50),
        Task(authorUuid = "user-uuid-002", projectId = "project-id-001", startTimestamp = timestamp, durationInSeconds = 130),
        Task(authorUuid = "user-uuid-002", projectId = "project-id-001", startTimestamp = timestamp, durationInSeconds = 60)
      )
    ))
  }

  def prepareDBForTasksTests: Future[Unit] = {
    val timestamp1 = LocalDateTime.parse("2022-04-09T10:55:21.908", formatter)
    val timestamp2 = LocalDateTime.parse("2022-04-09T10:35:21.908", formatter)

    Await.ready(db.run(allSchemas.dropIfExists), Duration.Inf)
    Await.ready(createSchemaIfNotExists, Duration.Inf)

    db.run(DBIO.seq(
      projects.delete, tasks.delete,

      projects ++= Seq(
        Project("project-id-001", "user-uuid-001"),
        Project("project-id-002", "user-uuid-002"),
        Project("project-id-003", "user-uuid-002")
      ),

      tasks ++= Seq(
        Task(authorUuid = "user-uuid-001", projectId = "project-id-003", startTimestamp = timestamp1, durationInSeconds = 50),
        Task(authorUuid = "user-uuid-002", projectId = "project-id-001", startTimestamp = timestamp1, durationInSeconds = 130),
        Task(authorUuid = "user-uuid-002", projectId = "project-id-001", startTimestamp = timestamp2, durationInSeconds = 60)
      )
    ))
  }
}
