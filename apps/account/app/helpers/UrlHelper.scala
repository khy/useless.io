package helpers.account

import play.api._

object UrlHelper {

  def authUrl = {
    val config = Play.current.configuration
    val authBaseUrl = config.getString("account.baseUrl").get
    val appGuid = config.getString("account.appGuid").get
    val scopes = "admin"
    authBaseUrl + s"/auth?app_guid=$appGuid&scopes=$scopes"
  }

}
