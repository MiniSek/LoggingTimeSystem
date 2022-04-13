package pl.agh.it.server.config

import pl.agh.it.database.models.{Project, Task}
import pl.agh.it.server.models._
import spray.json.DefaultJsonProtocol.{jsonFormat1, jsonFormat2, jsonFormat3, jsonFormat4, jsonFormat5, jsonFormat9}
import spray.json.{JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}
import spray.json.DefaultJsonProtocol._

import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

/*
  It's class for marshalling objects to jsons and unmarshalling jsons to objects.
*/
trait MarshallingUnmarshallingConfiguration extends LDTFormatterConfiguration {
  implicit val projectIdRequestFormat: RootJsonFormat[ProjectIdRequest] = jsonFormat1(ProjectIdRequest)
  implicit val changeProjectIdRequestFormat: RootJsonFormat[ChangeProjectIdRequest] =
    jsonFormat2(ChangeProjectIdRequest)
  implicit val addTaskRequestFormat: RootJsonFormat[AddTaskRequest] = jsonFormat5(AddTaskRequest)
  implicit val deleteTaskRequestFormat: RootJsonFormat[DeleteTaskRequest] = jsonFormat1(DeleteTaskRequest)
  implicit val changeTaskAttributesRequestFormat: RootJsonFormat[ChangeTaskAttributesRequest] =
    jsonFormat5(ChangeTaskAttributesRequest)

  implicit val localDateTimeFormat = new JsonFormat[LocalDateTime] {
    override def write(obj: LocalDateTime): JsValue = JsString(obj.format(formatter))

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(s) => Try(LocalDateTime.parse(s)) match {
        case Success(result) => result
        case Failure(exception) => deserializationError(s"could not parse $s as LocalDateTime", exception)
      }
      case notAJsString => deserializationError(s"expected a String but got a $notAJsString")
    }
  }

  implicit val projectFormat: RootJsonFormat[Project] = jsonFormat4(Project)
  implicit val taskFormat: RootJsonFormat[Task] = jsonFormat9(Task)
  implicit val projectTasksDurationResponseFormat: RootJsonFormat[ProjectTasksDurationResponse] =
    jsonFormat3(ProjectTasksDurationResponse)
  implicit val projectTasksResponse: RootJsonFormat[ProjectTasksResponse] = jsonFormat2(ProjectTasksResponse)
  implicit val allProjectsResponseFormat: RootJsonFormat[AllProjectsResponse] = jsonFormat1(AllProjectsResponse)
}
