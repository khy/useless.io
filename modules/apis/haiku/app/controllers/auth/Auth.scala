package controllers.haiku.auth

import play.api.Play.current

import io.useless.play.authentication.LegacyAuthenticated

object Auth extends LegacyAuthenticated("haiku.accessTokenGuid")
