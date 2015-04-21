package test.auth

import java.util.UUID
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.{ Application, Plugin }
import io.useless.accesstoken.{ AccessToken, Scope }
import io.useless.account.App
import io.useless.client.ClientConfiguration

import clients.accesstoken._
import clients.account._
import services.LoginService

class FunctionalSpec
  extends Specification
  with    ClientConfiguration
{

  val accountAppGuid = UUID.fromString("a198fb60-f97c-4837-a47b-d12eaae0d0f7")
  val authUrl = s"/auth?app_guid=${accountAppGuid}&scopes=admin"

  val guidRx = """[a-f\d]{8}-[a-f\d]{4}-[a-f\d]{4}-[a-f\d]{4}-[a-f\d]{12}"""
  val appRedirectRx = ("""http:\/\/account.useless.io\/auth\?code=""" + guidRx + "$").r

  def testEmail(username: String = UUID.randomUUID.toString) = {
    s"${username}@useless-auth-functional-test.dev"
  }

  "User first needs to create an account" in new WithBrowser(webDriver = FIREFOX) {
    browser.goTo(authUrl)
    browser.url must beEqualTo("/sign-in")
    browser.click("a.sign-up")

    val handle = UUID.randomUUID.toString
    browser.url must beEqualTo("/sign-up")
    browser.fill("input.name").`with`("Kevin Hyland")
    browser.fill("input.email").`with`(testEmail(handle))
    browser.fill("input.handle").`with`(handle)
    browser.fill("input.password").`with`("secret")
    browser.submit("form.login")

    browser.url must beEqualTo(authUrl)
    browser.findFirst("h2").getText must beEqualTo("Account would like to access your useless.io account. In particular, it has requested the following special permissions:")
    browser.findFirst("table.scope td.scope-name").getText must beEqualTo("Admin")
    browser.click("input.allow")

    browser.url must beMatching(appRedirectRx)
  }

  "User first needs to sign in, and does not have requested access token" in new WithBrowser(webDriver = FIREFOX) {
    val email = testEmail()
    LoginService.create(email, "secret", None, None)
    browser.goTo(authUrl)

    browser.url must beEqualTo("/sign-in")
    browser.fill("input.email").`with`(email)
    browser.fill("input.password").`with`("secret")
    browser.submit("form.session")

    browser.url must beEqualTo(authUrl)
    browser.findFirst("h2").getText must beEqualTo("Account would like to access your useless.io account. In particular, it has requested the following special permissions:")
    browser.findFirst("table.scope td.scope-name").getText must beEqualTo("Admin")
    browser.click("input.allow")

    browser.url must beMatching(appRedirectRx)
  }

  "User is already signed in, but does not have requested access token" in new WithBrowser(webDriver = FIREFOX) {
    val email = testEmail()
    LoginService.create(email, "secret", None, None)
    browser.goTo("/sign-in")
    browser.fill("input.email").`with`(email)
    browser.fill("input.password").`with`("secret")
    browser.submit("form.session")

    browser.goTo(authUrl)
    browser.url must beEqualTo(authUrl)
    browser.findFirst("h2").getText must beEqualTo("Account would like to access your useless.io account. In particular, it has requested the following special permissions:")
    browser.findFirst("table.scope td.scope-name").getText must beEqualTo("Admin")
    browser.click("input.allow")

    browser.url must beMatching(appRedirectRx)
  }

  "User first needs to sign in, but has an access token with the request scope" in new WithBrowser(webDriver = FIREFOX) {
    val email = testEmail()
    val result = LoginService.create(email, "secret", None, None)
    val userAccessToken = Helpers.await(result).right.get
    AccessTokenClient.withAuth(userAccessToken).createAccessToken(accountAppGuid, Seq(Scope("admin")))

    browser.goTo(authUrl)
    browser.url must beEqualTo("/sign-in")
    browser.fill("input.email").`with`(email)
    browser.fill("input.password").`with`("secret")
    browser.submit("form.session")

    browser.url must beMatching(appRedirectRx)
  }

  "User is already signed in and has an access token with the request scope" in new WithBrowser(webDriver = FIREFOX) {
    val email = testEmail()
    val result = LoginService.create(email, "secret", None, None)
    val userAccessToken = Helpers.await(result).right.get
    AccessTokenClient.withAuth(userAccessToken).createAccessToken(accountAppGuid, Seq(Scope("admin")))

    browser.goTo("/sign-in")
    browser.fill("input.email").`with`(email)
    browser.fill("input.password").`with`("secret")
    browser.submit("form.session")

    browser.goTo(authUrl)
    browser.url must beMatching(appRedirectRx)
  }

  "App requests an access token without scopes" in new WithBrowser(webDriver = FIREFOX) {
    val authUrlWithoutScope = s"/auth?app_guid=${accountAppGuid}"
    val email = testEmail()
    LoginService.create(email, "secret", None, None)

    browser.goTo(authUrlWithoutScope)
    browser.fill("input.email").`with`(email)
    browser.fill("input.password").`with`("secret")
    browser.submit("form.session")

    browser.findFirst("h2").getText must beEqualTo("Account would like to access your useless.io account. It has not requested any special permissions.")
    browser.click("input.allow")

    browser.url must beMatching(appRedirectRx)
  }

}
