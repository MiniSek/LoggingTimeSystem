package pl.agh.it.server

import pl.agh.it.database.config.{DatabaseHelper, DatabaseSchema}
import pl.agh.it.database.{ProjectsDao, TasksDao}

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.Duration
import scala.io.StdIn

/*
  Main app to run service.
*/
object TimeLoggingService extends App with DatabaseSchema with DatabaseHelper {
  val db = Database.forConfig("mysql")
  Await.ready(createSchemaIfNotExists, Duration.Inf)
  println("---Service ready---")

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "my-system")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val projectsDao: ProjectsDao = new ProjectsDao(db)
  val tasksDao: TasksDao = new TasksDao(db)

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(MainRoute.getMainRoute(projectsDao, tasksDao))
  println("---Print enter to shut down service---")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
}

