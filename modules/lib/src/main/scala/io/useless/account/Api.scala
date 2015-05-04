package io.useless.account

import java.util.UUID

trait Api extends Account {

  def key: String

}

class PublicApi(
  val guid: UUID,
  val key: String
) extends Api

object Api {

  def apply(guid: UUID, key: String): Api = {
    Api.public(guid, key)
  }

  def public(guid: UUID, key: String): PublicApi = {
    new PublicApi(guid, key)
  }

}
