package services.budget.util

import java.util.UUID

class ServiceException(msg: String) extends RuntimeException(msg)

class ResourceUnexpectedlyNotFound(resourceType: String, id: Any)
  extends ServiceException(s"Resource of type [${resourceType}] with ID [${id.toString}] was unexpectedly not found")
