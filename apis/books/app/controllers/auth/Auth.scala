package controllers.books.auth

import java.util.UUID
import play.api.Play

import io.useless.play.authentication.{ BaseAuthenticated, ClientAuthDaoComponent }

object Auth
  extends BaseAuthenticated
  with    ClientAuthDaoComponent
{

  val authDao = new ClientAuthDao(
    Play.current.configuration.getString("books.accessTokenGuid").map { raw =>
      UUID.fromString(raw)
    }
  )

}
