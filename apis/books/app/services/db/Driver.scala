package services.books.db

import slick.driver.PostgresDriver
import com.github.tminglei.slickpg._

private [services] object Driver
  extends PostgresDriver
  with PgSearchSupport
{

  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus

  trait ImplicitsPlus
    extends Implicits
    with SearchImplicits

  class SimpleQLPlus
    extends SimpleQL
    with ImplicitsPlus
    with SearchAssistants

}
