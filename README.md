# LoggingTimeSystem
 
It's REST API service for time logging. Technology used:
 - MySQL as database
 - Slick as database access library
 - Akka HTTp for HTTP request management

Run service and send endpoints.
Every request must contain JWT token which can be verified by service. Here are three of them for testing:
 - user-uuid-001 -> eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCIsIlVVSUQiOiJ1c2VyLXV1aWQtMDAxIn0.CGq0ri4JDkWSUWJadLo9EEHOG3pcnAx8cdMrKOxZFiA
 - user-uuid-002 -> eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCIsIlVVSUQiOiJ1c2VyLXV1aWQtMDAyIn0.x24PynKwFBgypw-IlCNUMFZyvgc5eZ1188kyfWJAUx0
 - user-uuid-003 -> eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCIsIlVVSUQiOiJ1c2VyLXV1aWQtMDAzIn0.-wFQYChHUhhU8c_UmjaGpMigGYG1ZkdUNBvoY7r7_20
Remember that header Access-Token must start with 'Bearer '

Available endpoints:
 1) Projects management:

  &ensp;- adding new project:
  
           path: /project
           method: POST
           body (application/json):
               id (required) - string
  &ensp;- changing project id:
   
           path: /project
           method: PUT
           body (application/json):
               oldId (required) - string
               newId (required) - string
  &ensp;- deleting project:
  
           path: /project
           method: DELETE
           body (application/json):
               id (required) - string
    
 2) Tasks management:

  &ensp;- adding new task:
  
           path: /task
           method: POST
           body (application/json):
               projectId (required) - string
               startTimestamp (required) - string [yyyy-MM-ddTHH:mm:ss.SSS]
               durationInSeconds (required) - integer
               volume (optional) - integer
               description (optional) - string
  &ensp;- changing task attributes:
   
           path: /task
           method: PUT
           body (application/json):
               id (required) - integer
               startTimestamp (optional) - string [yyyy-MM-ddTHH:mm:ss.SSS]
               durationInSeconds (optional) - integer
               volume (optional) - integer
               description (optional) - string
  &ensp;- deleting task:
  
           path: /task
           method: DELETE
           body (application/json):
               id (required) - integer
               
 3)  Projects info:

  &ensp;- getting project and its tasks:
  
           path: /info
           method: GET
           query parameters:
               id (required) - string
  &ensp;- getting all projects and its tasks with filtering:
   
           path: /info/all
           method: GET
           query parameters:
               page (optional, default = 1) - natural number
               pageSize (optional, default = 10) - natural number
               sortBy (optional) - string [CreationTime/UpdateTime/None]
               reverse (optional) - string [true/false]
               id (optional, repeated) - string
               fromDate (optional) - string [yyyy-MM-ddTHH:mm:ss.SSS]
               toDate (optional) - string [yyyy-MM-ddTHH:mm:ss.SSS]
               byDeleted (optional) - string [all/deleted/undeleted]
