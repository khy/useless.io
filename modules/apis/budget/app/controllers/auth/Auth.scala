package controllers.budget.auth

import play.api.Play.current

import io.useless.play.authentication.Authenticated

object Auth extends Authenticated("budget.accessTokenGuid")
