import java.util.UUID

package io.useless.exception.service {

  class ServiceException private [service] (msg: String) extends RuntimeException(msg)

  class InvalidState(msg: String) extends ServiceException(msg)

  class ResourceNotFound(resourceType: String, id: String, attribute: String = "ID")
    extends ServiceException("Resource of type [%s] with %s [%s] was unexpectedly not found".format(resourceType, attribute, id))
  {

    def this(resourceType: String, guid: UUID) = {
      this(resourceType, guid.toString, "GUID")
    }

  }

}
