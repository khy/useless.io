package io.useless

object Message {

  def apply(key: String, details: (String, String)*) = {
    new Message(key, Map(details:_*))
  }

}

class Message(
  val key: String,
  val details: Map[String, String]
)
