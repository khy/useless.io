package controllers.budget.auth

import play.api.Play.current

import io.useless.play.authentication.LegacyAuthenticated

object Auth extends LegacyAuthenticated("budget.accessTokenGuid")
