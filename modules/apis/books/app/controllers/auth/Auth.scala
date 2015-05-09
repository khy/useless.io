package controllers.books.auth

import play.api.Play.current

import io.useless.play.authentication.Authenticated

object Auth extends Authenticated("books.accessTokenGuid")
