package io.useless.account

import java.util.UUID

/**
 * Account models any entity that can make authenticated requests to useless.io.
 * In particular, this will be either an end user, a 3rd-party app, or a
 * useless.io API.
 *
 * @see Api
 * @see App
 * @see User
 */
trait Account {

  def guid: UUID

}
