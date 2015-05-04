package test.util

import org.scalatest._
import org.scalatestplus.play._

trait DefaultSpec
  extends PlaySpec
  with OneServerPerSuite
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with ClearDbBeforeEach
  with DefaultUselessMock
