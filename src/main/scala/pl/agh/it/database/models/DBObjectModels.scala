package pl.agh.it.database.models

import java.time.LocalDateTime

/*
  Project and Task are model classes.
  Project main components are
    - 'id' field delivered by user
    - 'creationTimestamp'
  Task main components are
    - 'startTimestamp'
    - 'durationInSeconds'
    - 'volume'
    - 'description'
*/
case class Project(id: String, authorUuid: String, creationTimestamp: LocalDateTime = LocalDateTime.now(),
                   deletedTimestamp: Option[LocalDateTime] = None)

case class Task(id: Option[Int] = None, authorUuid: String, projectId: String, startTimestamp: LocalDateTime,
                durationInSeconds: Long, volume: Option[Int] = None, description: Option[String] = None,
                creationTimestamp: LocalDateTime = LocalDateTime.now(), deletedTimestamp: Option[LocalDateTime] = None)