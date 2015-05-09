package controllers.haiku.auth

import play.api.Play.current

import io.useless.play.authentication.Authenticated

object Auth extends Authenticated("haiku.accessTokenGuid")
