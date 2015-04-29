package controllers.haiku.auth

import io.useless.play.authentication.Authenticated

object Auth extends Authenticated("haiku.accessTokenGuid")
