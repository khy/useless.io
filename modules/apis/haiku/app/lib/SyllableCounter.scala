package lib.haiku

import scala.io.Source
import scala.util.matching.Regex

trait SyllableCounter {

  def count(text: String): Option[Seq[Int]]

}

object TwoPhaseLineSyllableCounter {

  lazy val default = new TwoPhaseLineSyllableCounter(
    CmuDictSyllableCounter.default,
    NaiveHeuristicSyllableCounter
  )

}

class TwoPhaseLineSyllableCounter(
  primaryWordCounter: SyllableCounter,
  fallbackWordCounter: SyllableCounter
) extends SyllableCounter {

  def count(text: String) = {
    val syllableCounts: Seq[Option[Seq[Int]]] = text.split(" ").map { word =>
      primaryWordCounter.count(word).orElse {
        fallbackWordCounter.count(word)
      }
    }

    syllableCounts.reduce[Option[Seq[Int]]] { case (optSyllables1, optSyllables2) =>
      for {
        syllables1 <- optSyllables1
        syllables2 <- optSyllables2
      } yield Seq(
        (syllables1.min + syllables2.min),
        (syllables1.max + syllables2.max)
      )
    }
  }

}

object CmuDictSyllableCounter {

  lazy val default = {
    val source = Source.fromFile("conf/cmudict.0.6d.txt")
    new CmuDictSyllableCounter(source)
  }

}

// From here: http://www.onebloke.com/2011/06/counting-syllables-accurately-in-python-on-google-app-engine/
class CmuDictSyllableCounter(
  source: Source
) extends SyllableCounter {

  private val cmuDict = {
    val _map = scala.collection.mutable.Map[String, Seq[Int]]()
    val lineRx = new Regex("^([A-Z]+)  (.+)$", "word", "parts")
    val syllableRx = new Regex("""\d$""")

    source.getLines.foreach { line =>
      lineRx.findFirstMatchIn(line).foreach { rxMatch =>
        val word = rxMatch.group("word")
        val syllableCount = rxMatch.group("parts").split(" ").count { part =>
          syllableRx.findFirstIn(part).isDefined
        }

        val syllableCounts = _map.get(word).map { existingSyllableCounts =>
          existingSyllableCounts ++ Seq(syllableCount)
        }.getOrElse(Seq(syllableCount))

        _map += ((word, syllableCounts))
      }
    }

    _map
  }

  def count(text: String) = cmuDict.get(text.toUpperCase)

}

// From here: http://stackoverflow.com/questions/1271918/ruby-count-syllables
object NaiveHeuristicSyllableCounter extends SyllableCounter {

  def count(text: String) = {
    if (text.length <= 3) {
      Some(Seq(1))
    } else {
      var _text = text.toLowerCase
      _text = "(?:[^laeiouy]es|ed|[^laeiouy]e)$".r.replaceAllIn(_text, "")
      _text = "^y".r.replaceAllIn(_text, "")
      Some(Seq("[aeiouy]{1,2}".r.findAllIn(_text).length))
    }
  }

}
