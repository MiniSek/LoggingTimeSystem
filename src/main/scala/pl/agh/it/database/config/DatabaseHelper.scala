package pl.agh.it.database.config

import pl.agh.it.server.config.LDTFormatterConfiguration
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global


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
}
