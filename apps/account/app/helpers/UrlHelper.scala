package helpers.account

import play.api._

object UrlHelper {

  def authUrl = {
    val config = Play.current.configuration
    val authBaseUrl = config.getString("useless.auth.baseUrl").get
    val appGuid = config.getString("useless.account.guid").get
    val scopes = "admin"
    authBaseUrl + s"/auth?app_guid=$appGuid&scopes=$scopes"
  }

}
