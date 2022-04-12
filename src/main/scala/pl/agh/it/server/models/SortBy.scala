package pl.agh.it.server.models

/*
  Enumeration to model available sorts.
*/

object SortBy extends Enumeration {
  type SortByField = Value

  val CreationTime, UpdateTime, None = Value
}
