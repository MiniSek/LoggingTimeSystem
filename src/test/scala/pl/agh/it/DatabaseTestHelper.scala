package pl.agh.it

import pl.agh.it.database.config.{DatabaseHelper, DatabaseSchema}
import pl.agh.it.database.models.{Project, Task}
import pl.agh.it.server.config.LDTFormatterConfiguration
import slick.jdbc.MySQLProfile.api._

import java.time.LocalDateTime
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


trait DatabaseTestHelper extends LDTFormatterConfiguration with DatabaseHelper {
  self: DatabaseSchema =>

  def db: Database

  def prepareDBForProjectTests: Future[Unit] = {
    Await.ready(createSchemaIfNotExists, Duration.Inf)
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

  def prepareDBForTaskIntegrationTests: Future[Unit] = {
    Await.ready(db.run(allSchemas.dropIfExists), Duration.Inf)
    Await.ready(createSchemaIfNotExists, Duration.Inf)
    db.run(DBIO.seq(
      projects ++= Seq(
        Project("project-id-001", "user-uuid-001"),
        Project("project-id-002", "user-uuid-001")
      )
    ))
  }
}
