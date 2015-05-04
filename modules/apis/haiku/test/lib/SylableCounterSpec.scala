package lib.haiku

import java.util.UUID
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._

class SyllableCounterSpec extends Specification {

  "TwoPhaseLineSyllableCounter#count" should {

    val counter = TwoPhaseLineSyllableCounter.default

    "return roughly 5 syllables for 'A rhinoceros,'" in {
      val syllables = counter.count("A rhinoceros,").get
      (syllables.min - 1) must beLessThanOrEqualTo(5)
      (syllables.max + 1) must beGreaterThanOrEqualTo(5)
    }

    "return roughly 7 syllables for 'hanging, bright green, on my wall,'" in {
      val syllables = counter.count("hanging, bright green, on my wall,").get
      (syllables.min - 1) must beLessThanOrEqualTo(7)
      (syllables.max + 1) must beGreaterThanOrEqualTo(7)
    }

    "return roughly 5 syllables for 'is smoking a pipe'" in {
      val syllables = counter.count("is smoking a pipe").get
      (syllables.min - 1) must beLessThanOrEqualTo(5)
      (syllables.max + 1) must beGreaterThanOrEqualTo(5)
    }

  }

  "CmuDictSyllableCounter#count" should {

    val counter = CmuDictSyllableCounter.default

    "return a count of 3 syllables for 'telephone'" in {
      val syllables = counter.count("telephone").get
      syllables.min must beLessThanOrEqualTo(3)
      syllables.max must beGreaterThanOrEqualTo(3)
    }

    "return a count of 5 syllables for 'abolitionists'" in {
      val syllables = counter.count("abolitionist").get
      syllables.min must beLessThanOrEqualTo(5)
      syllables.max must beGreaterThanOrEqualTo(5)
    }

    "return None for 'yolo'" in {
      counter.count("yolo") must beNone
    }

  }

  "NaiveHeuristicSyllableCounter.count" should {

    "return a count of 2 syllables for 'yolo'" in {
      val syllables = NaiveHeuristicSyllableCounter.count("yolo").get
      syllables.min must beLessThanOrEqualTo(2)
      syllables.max must beGreaterThanOrEqualTo(2)
    }

  }

}
