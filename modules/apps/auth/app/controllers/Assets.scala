package controllers.auth

import play.api.http.LazyHttpErrorHandler

object Assets extends controllers.AssetsBuilder(LazyHttpErrorHandler)
