package test.auth

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.{ Application, Plugin }
import io.useless.accesstoken.{ AccessToken, Scope }
import io.useless.account.App

import clients.auth.accesstoken._
import clients.auth.account._
import services.auth.LoginService

class FunctionalSpec
  extends PlaySpec
  with OneServerPerSuite
  with OneBrowserPerSuite
  with FirefoxFactory
{

  val accountAppGuid = UUID.fromString("a198fb60-f97c-4837-a47b-d12eaae0d0f7")
  val authUrl = s"/auth?app_guid=${accountAppGuid}&scopes=admin"

  val guidRx = """[a-f\d]{8}-[a-f\d]{4}-[a-f\d]{4}-[a-f\d]{4}-[a-f\d]{12}"""
  val appRedirectRx = ("""http:\/\/account.useless.io\/auth\?code=""" + guidRx + "$").r

  def testEmail(username: String = UUID.randomUUID.toString) = {
    s"${username}@useless-auth-functional-test.dev"
  }

  if (false) {

    "User first needs to create an account" in {
      goTo(authUrl)
      currentUrl mustBe "/sign-in"
      clickOn("a.sign-up")

      val handle = UUID.randomUUID.toString
      currentUrl mustBe "/sign-up"
      textField("input.name").value = "Kevin Hyland"
      textField("input.email").value = testEmail(handle)
      textField("input.handle").value = handle
      textField("input.password").value = "secret"
      submit()

      currentUrl mustBe authUrl
      find("h2").get.text mustBe "Account would like to access your useless.io account. In particular, it has requested the following special permissions:"
      find("table.scope td.scope-name").get.text mustBe "Admin"
      clickOn("input.allow")

      currentUrl must fullyMatch regex (appRedirectRx)
    }

    "User first needs to sign in, and does not have requested access token" in {
      val email = testEmail()
      LoginService.create(email, "secret", None, None)
      goTo(authUrl)

      currentUrl mustBe "/sign-in"
      textField("input.email").value = email
      textField("input.password").value = "secret"
      submit()

      currentUrl mustBe authUrl
      find("h2").get.text mustBe "Account would like to access your useless.io account. In particular, it has requested the following special permissions:"
      find("table.scope td.scope-name").get.text mustBe "Admin"
      clickOn("input.allow")

      currentUrl must fullyMatch regex (appRedirectRx)
    }

    "User is already signed in, but does not have requested access token" in {
      val email = testEmail()
      LoginService.create(email, "secret", None, None)
      goTo("/sign-in")
      textField("input.email").value = email
      textField("input.password").value = "secret"
      submit()

      goTo(authUrl)
      currentUrl mustBe authUrl
      find("h2").get.text mustBe "Account would like to access your useless.io account. In particular, it has requested the following special permissions:"
      find("table.scope td.scope-name").get.text mustBe "Admin"
      clickOn("input.allow")

      currentUrl must fullyMatch regex (appRedirectRx)
    }

    "User first needs to sign in, but has an access token with the request scope" in {
      val email = testEmail()
      val result = LoginService.create(email, "secret", None, None)
      val userAccessToken = Helpers.await(result).right.get
      AccessTokenClient.instance(Some(userAccessToken)).createAccessToken(accountAppGuid, Seq(Scope("admin")))

      goTo(authUrl)
      currentUrl mustBe "/sign-in"
      textField("input.email").value = email
      textField("input.password").value = "secret"
      submit()

      currentUrl must fullyMatch regex (appRedirectRx)
    }

    "User is already signed in and has an access token with the request scope" in {
      val email = testEmail()
      val result = LoginService.create(email, "secret", None, None)
      val userAccessToken = Helpers.await(result).right.get
      AccessTokenClient.instance(Some(userAccessToken)).createAccessToken(accountAppGuid, Seq(Scope("admin")))

      goTo("/sign-in")
      textField("input.email").value = email
      textField("input.password").value = "secret"
      submit()

      goTo(authUrl)
      currentUrl must fullyMatch regex (appRedirectRx)
    }

    "App requests an access token without scopes" in {
      val authUrlWithoutScope = s"/auth?app_guid=${accountAppGuid}"
      val email = testEmail()
      LoginService.create(email, "secret", None, None)

      goTo(authUrlWithoutScope)
      textField("input.email").value = email
      textField("input.password").value = "secret"
      submit()

      find("h2").get.text mustBe "Account would like to access your useless.io account. It has not requested any special permissions."
      clickOn("input.allow")

      currentUrl must fullyMatch regex (appRedirectRx)
    }

  }

}
