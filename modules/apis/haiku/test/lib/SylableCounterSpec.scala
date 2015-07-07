package lib.haiku

import java.util.UUID
import org.scalatestplus.play.{PlaySpec, OneAppPerSuite}
import play.api.test._
import play.api.test.Helpers._

class SyllableCounterSpec extends PlaySpec with OneAppPerSuite {

  "TwoPhaseLineSyllableCounter#count" must {

    val counter = TwoPhaseLineSyllableCounter.default

    "return roughly 5 syllables for 'A rhinoceros,'" in {
      val syllables = counter.count("A rhinoceros,").get
      (syllables.min - 1) must be <= (5)
      (syllables.max + 1) must be >= (5)
    }

    "return roughly 7 syllables for 'hanging, bright green, on my wall,'" in {
      val syllables = counter.count("hanging, bright green, on my wall,").get
      (syllables.min - 1) must be <= (7)
      (syllables.max + 1) must be >= (7)
    }

    "return roughly 5 syllables for 'is smoking a pipe'" in {
      val syllables = counter.count("is smoking a pipe").get
      (syllables.min - 1) must be <= (5)
      (syllables.max + 1) must be >= (5)
    }

  }

  "CmuDictSyllableCounter#count" must {

    val counter = CmuDictSyllableCounter.default

    "return a count of 3 syllables for 'telephone'" in {
      val syllables = counter.count("telephone").get
      syllables.min must be <= (3)
      syllables.max must be >= (3)
    }

    "return a count of 5 syllables for 'abolitionists'" in {
      val syllables = counter.count("abolitionist").get
      syllables.min must be <= (5)
      syllables.max must be >= (5)
    }

    "return None for 'yolo'" in {
      counter.count("yolo") mustBe None
    }

  }

  "NaiveHeuristicSyllableCounter.count" must {

    "return a count of 2 syllables for 'yolo'" in {
      val syllables = NaiveHeuristicSyllableCounter.count("yolo").get
      syllables.min must be <= (2)
      syllables.max must be >= (2)
    }

  }

}
