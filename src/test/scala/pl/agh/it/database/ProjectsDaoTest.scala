package pl.agh.it.database

import org.scalatest._
import flatspec._
import pl.agh.it.DatabaseTestHelper
import pl.agh.it.database.config.DatabaseSchema
import pl.agh.it.database.models.{DeleteProjectException, Project, Task}
import slick.jdbc.JdbcBackend.Database

import java.time.LocalDateTime
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class ProjectsDaoTest extends AsyncFlatSpec with DatabaseSchema with DatabaseTestHelper {
  val db = Database.forConfig("mysql")
  Await.ready(createSchemaIfNotExists, Duration.Inf)
  val projectsDao: ProjectsDao = new ProjectsDao(db)
  Await.ready(prepareDBForProjectTests, Duration.Inf)

  behavior of "getAllProjects"
  it should "eventually return sequence of projects" in {
    val future: Future[Seq[Project]] = projectsDao.getAllProjects
    future map { allProjects => {
      assert(allProjects.size == 3)
      assert(allProjects.head.id == "project-id-001")
      assert(allProjects(1).id == "project-id-002")
      assert(allProjects(2).id == "project-id-003")
    } }
  }

  behavior of "getProjectWithTasks"
  it should "eventually return tuple of project and its tasks" in {
    val future: Future[(Project, Seq[Task])] = projectsDao.getProjectWithTasks("project-id-003")
    future map { tuple => {
      assert(tuple._1.id == "project-id-003")
      assert(tuple._2.size == 1)
      assert(tuple._2.head.projectId == "project-id-003")
    }
    }
  }
  it should "eventually call exception due to lack of such project" in {
    recoverToSucceededIf[java.util.NoSuchElementException] {
      projectsDao.getProjectWithTasks("project-id-trash")
    }
  }

  behavior of "getProjectWithTasksAndDuration"
  it should "eventually return tuple of project, its tasks and duration of project" in {
    val future: Future[(Project, Seq[Task], Long)] = projectsDao.getProjectWithTasksAndDuration("project-id-001")
    future map { tuple => {
      assert(tuple._1.id == "project-id-001")
      assert(tuple._2.size == 2)
      assert(tuple._3 == 190)
    }
    }
  }
  it should "eventually call exception due to lack of such project" in {
    recoverToSucceededIf[java.util.NoSuchElementException] {
      projectsDao.getProjectWithTasksAndDuration("project-id-trash")
    }
  }

  behavior of "addProject"
  it should "eventually insert project to db" in {
    val future: Future[Int] = projectsDao.addProject(Project("user-uuid-003", "project-id-004"))
    future map { inserted => assert(inserted == 1) }
  }
  it should "eventually call exception" in {
    recoverToSucceededIf[java.sql.SQLIntegrityConstraintViolationException] {
      projectsDao.addProject(Project("user-uuid-003", "project-id-001"))
    }
  }

  behavior of "changeProjectId"
  it should "eventually change project id" in {
    val future: Future[Int] =
      projectsDao.changeProjectId("user-uuid-002", "project-id-002", "project-id-002-changed")
    future map { changed => assert(changed == 1) }
  }
  it should "eventually not change project id due to authority of project" in {
    val future: Future[Int] =
      projectsDao.changeProjectId("user-uuid-001", "project-id-003", "project-id-003-changed")
    future map { changed => assert(changed == 0) }
  }
  it should "eventually not change project id due to lack of such project" in {
    val future: Future[Int] =
      projectsDao.changeProjectId("user-uuid-001", "project-id-001-extended", "project-id-001-extended-changed")
    future map { changed => assert(changed == 0) }
  }
  it should "eventually call exception due to conflict of new id with another" in {
    recoverToSucceededIf[java.sql.SQLIntegrityConstraintViolationException] {
      projectsDao.changeProjectId("user-uuid-002", "project-id-003", "project-id-001")
    }
  }

  behavior of "softDelete"
  it should "eventually soft delete project with its tasks" in {
    val future: Future[Int] =
      projectsDao.softDelete("user-uuid-001", "project-id-001")
    future map { deletedTasks =>assert(deletedTasks == 2)}
  }
  it should "eventually call exception due to project's authority" in {
    recoverToSucceededIf[DeleteProjectException] {
      projectsDao.softDelete("user-uuid-001", "project-id-003")
    }
  }
  it should "eventually call exception due to earlier removal" in {
    recoverToSucceededIf[DeleteProjectException] {
      projectsDao.softDelete("user-uuid-001", "project-id-001")
    }
  }

  behavior of "getAllProjectsWithUpdateTime"
  it should "eventually return all projects with its update time" in {
    val future: Future[Seq[(Project, LocalDateTime)]] = projectsDao.getAllProjectsWithUpdateTime
    future map { allProjects => {
      assert(allProjects(2)._1.id == "project-id-003")
      assert(allProjects(1)._2 == allProjects(1)._1.creationTimestamp)
      assert(allProjects(2)._2 != allProjects(2)._1.creationTimestamp)
    }}
  }
}
