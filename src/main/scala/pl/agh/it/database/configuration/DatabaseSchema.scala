package pl.agh.it.database.configuration

import pl.agh.it.database.models.{Project, Task}
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._
import java.time.LocalDateTime

/*
  Slick schemas of Projects and Tasks tables in database with proper fields.
*/

trait DatabaseSchema {
  class Projects(tag: Tag) extends Table[Project](tag, "Projects") {
    def id = column[String]("ID", O.PrimaryKey)

    def authorUuid = column[String]("AUTHOR_UUID")

    def creationTimestamp = column[LocalDateTime]("CREATED_AT")

    def deletedTimestamp = column[Option[LocalDateTime]]("DELETED_AT")

    def * = (id, authorUuid, creationTimestamp, deletedTimestamp) <>(Project.tupled, Project.unapply)
  }

  val projects = TableQuery[Projects]

  class Tasks(tag: Tag) extends Table[Task](tag, "Tasks") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def authorUuid = column[String]("AUTHOR_UUID")

    def projectId = column[String]("PROJECT_ID")

    def startTimestamp = column[LocalDateTime]("STARTED_AT")

    def durationInSeconds = column[Long]("DURATION_IN_SECONDS")

    def volume = column[Option[Int]]("VOLUME")

    def description = column[Option[String]]("DESCRIPTION")

    def creationTimestamp = column[LocalDateTime]("CREATED_AT")

    def deletedTimestamp = column[Option[LocalDateTime]]("DELETED_AT")

    def lastUpdate = column[LocalDateTime]("UPDATED_AT")

    def project = foreignKey("FK_PROJECT", projectId, projects)(_.id)

    def * = (id.?, authorUuid, projectId, startTimestamp, durationInSeconds, volume, description, creationTimestamp,
      deletedTimestamp) <>(Task.tupled, Task.unapply)
  }

  val tasks = TableQuery[Tasks]

  val allSchemas = projects.schema ++ tasks.schema
}
