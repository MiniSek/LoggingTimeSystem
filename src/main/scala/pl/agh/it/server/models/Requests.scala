package pl.agh.it.server.models

import pl.agh.it.database.models.{Project, Task}

/*
  ProjectIdRequest serves to model user request to add and delete project, for those actions 'id' field is enough.
*/
final case class ProjectIdRequest(id: String)
final case class ChangeProjectIdRequest(oldId: String, newId: String)

final case class AddTaskRequest(projectId: String, startTimestamp: String, durationInSeconds: Long,
                                volume: Option[Int] = None, description: Option[String] = None)
final case class DeleteTaskRequest(id: Int)
final case class ChangeTaskAttributesRequest(id: Int, startTimestamp: Option[String], durationInSeconds: Option[Long],
                                             volume: Option[Int] = None, description: Option[String] = None)

/*
  ProjectTasksDurationResponse serves for response of get project info
  AllProjectsResponse and ProjectTasksResponse serves for response of get all projects with its tasks (and some filters)
*/
final case class ProjectTasksDurationResponse(project: Project, tasks: Seq[Task], duration: Long)
final case class ProjectTasksResponse(project: Project, tasks: Seq[Task])
final case class AllProjectsResponse(projects: Seq[ProjectTasksResponse])