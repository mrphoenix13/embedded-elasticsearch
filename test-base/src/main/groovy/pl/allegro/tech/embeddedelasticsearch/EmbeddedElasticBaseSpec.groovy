package pl.allegro.tech.embeddedelasticsearch

import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.*

abstract class EmbeddedElasticBaseSpec extends Specification {

    def "should index document"() {
        when:
            index(FIAT_126p)

        then:
            final result = searchByTerm(CARS_INDEX_NAME, CAR_INDEX_TYPE, "model", FIAT_126p.model)
            result.size() == 1
            assertJsonsEquals(toJson(FIAT_126p), result[0])
    }

    def "should index multiple documents without explicit id"() {
        when:
            index(CUJO)
            index(MISERY)
            index(SHINING)

        then:
            final result = fetchAllDocuments(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE)
            result.size() == 3
    }

    def "should index document with id"() {
        given:
            final id = "some-id"
            final document = toJson(FIAT_126p)

        when:
            embeddedElastic.index(CARS_INDEX_NAME, CAR_INDEX_TYPE, ["$id": document])

        then:
            final result = getById(CARS_INDEX_NAME, CAR_INDEX_TYPE, id)
            result != null
            assertJsonsEquals(document, result)
    }

    def "should recreate only specified index"() {
        given: "indices with some documents"
            index(FIAT_126p)
            index(SHINING)
            index(AMERICAN_PSYCHO)

        when: "recreating index"
            embeddedElastic.recreateIndex(BOOKS_INDEX_NAME)

        then: "recreated index should be empty"
            fetchAllDocuments(BOOKS_INDEX_NAME).size() == 0

        and: "other index should be untouched"
            fetchAllDocuments(CARS_INDEX_NAME).size() == 1
    }

    def "should recreate all indices"() {
        given: "indices with some documents"
            index(FIAT_126p)
            index(SHINING)
            index(AMERICAN_PSYCHO)

        when: "recreating index"
            embeddedElastic.recreateIndices()

        then: "recreated indices should be empty"
            fetchAllDocuments().size() == 0
    }

    def "should place document under right index and type"() {
        when:
            index(SHINING)
            index(AMERICAN_PSYCHO)

        then:
            with(fetchAllDocuments(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE)) { List<String> it ->
                it.size() == 1
                assertJsonsEquals(toJson(SHINING), it[0])
            }

        and:
            with(fetchAllDocuments(BOOKS_INDEX_NAME, AUDIO_BOOK_INDEX_TYPE)) { List<String> it ->
                it.size() == 1
                assertJsonsEquals(toJson(AMERICAN_PSYCHO), it[0])
            }
    }

    void assertJsonsEquals(String expectedJson, String actualJson) {
        JSONAssert.assertEquals(expectedJson, actualJson, false)
    }

    void index(Car car) {
        embeddedElastic.index(CARS_INDEX_NAME, CAR_INDEX_TYPE, toJson(car))
    }

    void index(PaperBook book) {
        embeddedElastic.index(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, toJson(book))
    }

    void index(AudioBook book) {
        embeddedElastic.index(BOOKS_INDEX_NAME, AUDIO_BOOK_INDEX_TYPE, toJson(book))
    }

    abstract EmbeddedElastic getEmbeddedElastic()

    abstract List<String> fetchAllDocuments()

    abstract List<String> fetchAllDocuments(String indexName)

    abstract List<String> fetchAllDocuments(String indexName, String typeName)

    abstract List<String> searchByTerm(String indexName, String typeName, String fieldName, String value)

    abstract String getById(String indexName, String indexType, String id)
}
