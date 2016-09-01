package clients.books

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.LocalDate

import models.books.Provider

class GoogleEditionClientSpec extends WordSpec with MustMatchers {

  "GoogleEditionClient.toEdition" must {

    "transform a Google response for 'The Marriage Plot' into an Edition" in {
      val googleJson = Json.parse(marriagePlotRawJson).as[JsObject]
      val edition = GoogleEditionClient.toEdition(googleJson).head
      edition.isbn mustBe "9781429969185"
      edition.title mustBe "The Marriage Plot"
      edition.subtitle mustBe Some("A Novel")
      edition.authors mustBe Seq("Jeffrey Eugenides")
      edition.pageCount mustBe 416
      edition.smallImageUrl mustBe Some("http://books.google.com/books/content?id=YPOixQM7VN0C&printsec=frontcover&img=1&zoom=5&edge=curl&source=gbs_api")
      edition.largeImageUrl mustBe Some("http://books.google.com/books/content?id=YPOixQM7VN0C&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api")
      edition.publisher mustBe Some("Macmillan")
      edition.publishedAt mustBe Some(LocalDate.parse("2011-10-11"))
      edition.provider mustBe Provider.Google
      edition.providerId mustBe Some("YPOixQM7VN0C")
    }

  }

  val marriagePlotRawJson = """
{
  "items": [
    {
      "kind": "books#volume",
      "id": "YPOixQM7VN0C",
      "etag": "/2A8N0d7Sb8",
      "selfLink": "https://www.googleapis.com/books/v1/volumes/YPOixQM7VN0C",
      "volumeInfo": {
        "title": "The Marriage Plot",
        "subtitle": "A Novel",
        "authors": ["Jeffrey Eugenides"],
        "publisher": "Macmillan",
        "publishedDate": "2011-10-11",
        "description": "A New York Times Notable Book of 2011 A Publisher's Weekly Top 10 Book of 2011 A Kirkus Reviews Top 25 Best Fiction of 2011 Title One of Library Journal's Best Books of 2011 A Salon Best Fiction of 2011 title One of The Telegraph's Best Fiction Books of the Year 2011 It's the early 1980s—the country is in a deep recession, and life after college is harder than ever. In the cafés on College Hill, the wised-up kids are inhaling Derrida and listening to Talking Heads. But Madeleine Hanna, dutiful English major, is writing her senior thesis on Jane Austen and George Eliot, purveyors of the marriage plot that lies at the heart of the greatest English novels. As Madeleine tries to understand why \"it became laughable to read writers like Cheever and Updike, who wrote about the suburbia Madeleine and most of her friends had grown up in, in favor of reading the Marquis de Sade, who wrote about deflowering virgins in eighteenth-century France,\" real life, in the form of two very different guys, intervenes. Leonard Bankhead—charismatic loner, college Darwinist, and lost Portland boy—suddenly turns up in a semiotics seminar, and soon Madeleine finds herself in a highly charged erotic and intellectual relationship with him. At the same time, her old \"friend\" Mitchell Grammaticus—who's been reading Christian mysticism and generally acting strange—resurfaces, obsessed with the idea that Madeleine is destined to be his mate. Over the next year, as the members of the triangle in this amazing, spellbinding novel graduate from college and enter the real world, events force them to reevaluate everything they learned in school. Leonard and Madeleine move to a biology Laboratory on Cape Cod, but can't escape the secret responsible for Leonard's seemingly inexhaustible energy and plunging moods. And Mitchell, traveling around the world to get Madeleine out of his mind, finds himself face-to-face with ultimate questions about the meaning of life, the existence of God, and the true nature of love. Are the great love stories of the nineteenth century dead? Or can there be a new story, written for today and alive to the realities of feminism, sexual freedom, prenups, and divorce? With devastating wit and an abiding understanding of and affection for his characters, Jeffrey Eugenides revives the motivating energies of the Novel, while creating a story so contemporary and fresh that it reads like the intimate journal of our own lives.",
        "industryIdentifiers": [{
          "type": "ISBN_13",
          "identifier": "9781429969185"
        }, {
          "type": "ISBN_10",
          "identifier": "1429969180"
        }],
        "readingModes": {
          "text": true,
          "image": false
        },
        "pageCount": 416,
        "printType": "BOOK",
        "categories": ["Fiction"],
        "averageRating": 3,
        "ratingsCount": 50,
        "maturityRating": "NOT_MATURE",
        "allowAnonLogging": true,
        "contentVersion": "1.11.8.0.preview.2",
        "imageLinks": {
          "smallThumbnail": "http://books.google.com/books/content?id=YPOixQM7VN0C&printsec=frontcover&img=1&zoom=5&edge=curl&source=gbs_api",
          "thumbnail": "http://books.google.com/books/content?id=YPOixQM7VN0C&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"
        },
        "language": "en",
        "previewLink": "http://books.google.com/books?id=YPOixQM7VN0C&printsec=frontcover&dq=The+Marriage+Plot&hl=&cd=2&source=gbs_api",
        "infoLink": "http://books.google.com/books?id=YPOixQM7VN0C&dq=The+Marriage+Plot&hl=&source=gbs_api",
        "canonicalVolumeLink": "http://books.google.com/books/about/The_Marriage_Plot.html?hl=&id=YPOixQM7VN0C"
      },
      "saleInfo": {
        "country": "US",
        "saleability": "FOR_SALE",
        "isEbook": true,
        "listPrice": {
          "amount": 9.99,
          "currencyCode": "USD"
        },
        "retailPrice": {
          "amount": 9.99,
          "currencyCode": "USD"
        },
        "buyLink": "http://books.google.com/books?id=YPOixQM7VN0C&dq=The+Marriage+Plot&hl=&buy=&source=gbs_api",
        "offers": [{
          "finskyOfferType": 1,
          "listPrice": {
            "amountInMicros": 9990000,
            "currencyCode": "USD"
          },
          "retailPrice": {
            "amountInMicros": 9990000,
            "currencyCode": "USD"
          },
          "giftable": true
        }]
      },
      "accessInfo": {
        "country": "US",
        "viewability": "PARTIAL",
        "embeddable": true,
        "publicDomain": false,
        "textToSpeechPermission": "ALLOWED",
        "epub": {
          "isAvailable": true,
          "acsTokenLink": "http://books.google.com/books/download/The_Marriage_Plot-sample-epub.acsm?id=YPOixQM7VN0C&format=epub&output=acs4_fulfillment_token&dl_type=sample&source=gbs_api"
        },
        "pdf": {
          "isAvailable": false
        },
        "webReaderLink": "http://books.google.com/books/reader?id=YPOixQM7VN0C&hl=&printsec=frontcover&output=reader&source=gbs_api",
        "accessViewStatus": "SAMPLE",
        "quoteSharingAllowed": false
      },
      "searchInfo": {
        "textSnippet": "With devastating wit and an abiding understanding of and affection for his characters, Jeffrey Eugenides revives the motivating energies of the Novel, while creating a story so contemporary and fresh that it reads like the intimate journal ..."
      }
    }
  ]
}
  """

}
