package services.auth

import java.util.UUID
import scala.concurrent.Future
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken
import io.useless.account.User
import org.mindrot.jbcrypt.BCrypt

import clients.auth.accesstoken.AccessTokenClient
import clients.auth.account.AccountClient
import daos.auth.LoginDao

object LoginService {

  lazy val accessTokenClient = AccessTokenClient.instance()

  lazy val accountClient = AccountClient.instance()

  lazy val dao = LoginDao.instance

  def auth(email: String, password: String): Future[Option[AccessToken]] = {
    accountClient.getUserForEmail(email).flatMap { optUser =>
      optUser.map { user =>
        dao.getLoginForUserGuid(user.guid).flatMap { optLoginDocument =>
          optLoginDocument.filter { loginDocument =>
            BCrypt.checkpw(password, loginDocument.hashedPassword)
          }.map { loginDocument =>
            accessTokenClient.getAccessToken(loginDocument.accessTokenGuid)
          }.getOrElse {
            Logger.info(s"Incorrect password for email [$email]")
            Future.successful(None)
          }
        }
      }.getOrElse {
        Logger.info(s"Unknown email [$email]")
        Future.successful(None)
      }
    }
  }

  def create(
    email: String,
    password: String,
    handle: Option[String],
    name: Option[String]
  ): Future[Either[String, AccessToken]] = {
    accountClient.getUserForEmail(email).flatMap { optUser =>
      optUser.map { user =>
        dao.getLoginForUserGuid(user.guid).flatMap { optLogin =>
          optLogin.map { login =>
            Future.successful(Left("A login for this email already exists."))
          }.getOrElse {
            createLogin(user, password)
          }
        }
      }.getOrElse {
        accountClient.createUser(email, handle, name).flatMap { result =>
          result.fold(
            error => Future.successful(Left(error)),
            user => createLogin(user, password)
          )
        }
      }
    }
  }

  private def createLogin(user: User, password: String): Future[Either[String, AccessToken]] = {
    accessTokenClient.createAdminAccessTokenForAccount(user.guid).flatMap { result =>
      result.fold(
        error => Future.successful(Left(error)),
        accessToken => dao.createLogin(user.guid, accessToken.guid, password).map { result =>
          result.right.map { _ => accessToken }
        }
      )
    }
  }

}
