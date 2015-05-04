package controllers.books.auth

import io.useless.play.authentication.Authenticated

object Auth extends Authenticated("books.accessTokenGuid")
