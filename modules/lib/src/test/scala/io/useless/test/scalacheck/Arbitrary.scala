package io.useless.test.scalacheck

import org.{ scalacheck => sc }
import sc.Gen._
import sc.Arbitrary.arbitrary

import java.util.UUID

import io.useless.ClientError
import io.useless.accesstoken._
import io.useless.account._
import io.useless.util.Validator

object Arbitrary {

  implicit val arbUuid: sc.Arbitrary[UUID] = sc.Arbitrary(const(UUID.randomUUID))

  implicit val arbClientError: sc.Arbitrary[ClientError] = sc.Arbitrary {
    for {
      key <- alphaStr
      details <- sc.Gen.containerOf[List,(String, String)](arbitrary[(String, String)])
    } yield ClientError(key, details:_*)
  }

  implicit val arbPublicApi: sc.Arbitrary[Api] = sc.Arbitrary {
    for {
      guid <- arbitrary[UUID]
      key <- alphaStr
    } yield Api.public(guid, key)
  }

  implicit val arbPublicApp: sc.Arbitrary[App] = sc.Arbitrary {
    for {
      guid <- arbitrary[UUID]
      name <- alphaStr
      url <- alphaStr
    } yield App.public(guid, name, url)
  }

  implicit val arbAuthorizedApp: sc.Arbitrary[AuthorizedApp] = sc.Arbitrary {
    for {
      guid <- arbitrary[UUID]
      name <- alphaStr
      url <- alphaStr
      authRedirectUrl <- alphaStr
    } yield App.authorized(guid, name, url, authRedirectUrl)
  }

  implicit val arbPublicUser: sc.Arbitrary[User] = sc.Arbitrary {
    for {
      guid <- arbitrary[UUID]
      handle <- Gen.handle
      name <- Gen.optionOf(alphaStr)
    } yield User.public(guid, handle, name)
  }

  implicit val arbAuthorizedUser: sc.Arbitrary[AuthorizedUser] = sc.Arbitrary {
    for {
      guid <- arbitrary[UUID]
      email <- Gen.email
      handle <- Gen.handle
      name <- Gen.optionOf(alphaStr)
    } yield User.authorized(guid, email, handle, name)
  }

  implicit val arbScope: sc.Arbitrary[Scope] = sc.Arbitrary {
    for {
      key <- alphaStr
      context <- Gen.optionOf(alphaStr)
    } yield new BaseScope(key, context)
  }

  implicit val arbPublicAccessToken: sc.Arbitrary[AccessToken] = sc.Arbitrary {
    for {
      guid <- arbitrary[UUID]
      resourceOwner <- oneOf(arbitrary[Api], arbitrary[App], arbitrary[User])
      client <- Gen.optionOf(oneOf(arbitrary[Api], arbitrary[App]))
      scopes <- containerOf[List,Scope](arbitrary[Scope])
    } yield AccessToken.public(guid, resourceOwner, client, scopes)
  }

  implicit val arbAuthorizedAccessToken: sc.Arbitrary[AuthorizedAccessToken] = sc.Arbitrary {
    for {
      guid <- arbitrary[UUID]
      authorizationCode <- arbitrary[UUID]
      resourceOwner <- oneOf(arbitrary[Api], arbitrary[App], arbitrary[User])
      client <- Gen.optionOf(oneOf(arbitrary[Api], arbitrary[App]))
      scopes <- containerOf[List,Scope](arbitrary[Scope])
    } yield AccessToken.authorized(guid, authorizationCode, resourceOwner, client, scopes)
  }

}
