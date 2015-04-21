package services.books

import org.scalatest._

class BaseServiceSpec
  extends WordSpec
  with MustMatchers
{

  "BaseService.scrubTsQuery" must {

    "return an appropriate ts_query for multiple words" in {
      BaseService.scrubTsQuery("Jonathan Franzen") mustBe "Jonathan:*|Franzen:*"
    }

    "handle multiple spaces in an input query with multiple words" in {
      BaseService.scrubTsQuery(" Jonathan  Franzen    ") mustBe "Jonathan:*|Franzen:*"
    }

    "return an appropriate query for a single word" in {
      BaseService.scrubTsQuery("Ames") mustBe "Ames:*"
    }

    "remove special characters from the input query" in {
      BaseService.scrubTsQuery("Eu!ge|nid&es") mustBe "Eugenides:*"
    }

  }

}
