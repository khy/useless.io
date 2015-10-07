package services.budget.util

import java.util.UUID

import io.useless.validation.Validation

class ServiceException(msg: String) extends RuntimeException(msg)

class ResourceUnexpectedlyNotFound(resourceType: String, id: Any)
  extends ServiceException(s"Resource of type [${resourceType}] with ID [${id.toString}] was unexpectedly not found")

class UnexpectedValidationFailure(failure: Validation.Failure[_])
  extends ServiceException(s"A validation unexpectedly failed [${failure}]")
