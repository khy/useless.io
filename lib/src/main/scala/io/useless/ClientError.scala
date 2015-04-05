package io.useless

object ClientError {

  def apply(key: String, details: (String, String)*) = {
    new ClientError(key, Map(details:_*))
  }

}

class ClientError(
  val key: String,
  val details: Map[String, String]
)
