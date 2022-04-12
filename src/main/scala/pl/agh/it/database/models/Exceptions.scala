package pl.agh.it.database.models

/*
  DeleteProjectException and NoSuchProjectException are model classes for descriptive exception handling in DAO.
*/

final case class DeleteProjectException(private val message: String = "", private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

final case class NoSuchProjectException(private val message: String = "", private val cause: Throwable = None.orNull)
  extends Exception(message, cause)
