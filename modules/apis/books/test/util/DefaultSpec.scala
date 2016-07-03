package test.util

import play.api.test.FakeApplication
import org.scalatest._
import org.scalatestplus.play._

trait DefaultSpec
  extends PlaySpec
  with OneServerPerSuite
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with ClearDbBeforeEach
  with DefaultUselessMock
{

  implicit override lazy val app: FakeApplication = {
    FakeApplication(additionalConfiguration = Map("application.router" -> "books.Routes"))
  }

}
