package services.books

import java.util.UUID

class ServiceException(msg: String) extends RuntimeException(msg)

class ResourceUnexpectedlyNotFound(resourceType: String, guid: UUID, attribute: String = "GUID")
  extends ServiceException("Resource of type [%s] with %s [%s] was unexpectedly not found".format(resourceType, attribute, guid.toString))
