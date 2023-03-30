package no.nav.bidrag.dokument.arkiv.consumer

import com.netflix.graphql.dgs.client.CustomGraphQLClient
import com.netflix.graphql.dgs.client.GraphQLError
import com.netflix.graphql.dgs.client.GraphQLResponse
import com.netflix.graphql.dgs.client.HttpResponse
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer.NotFoundException
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsInfo
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.model.JournalIkkeFunnetException
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.arkiv.model.ReasonToHttpStatus
import no.nav.bidrag.dokument.arkiv.model.SafException
import no.nav.bidrag.dokument.arkiv.query.DistribusjonInfoQuery
import no.nav.bidrag.dokument.arkiv.query.DokumentoversiktFagsakQuery
import no.nav.bidrag.dokument.arkiv.query.GraphQuery
import no.nav.bidrag.dokument.arkiv.query.JournalpostQuery
import no.nav.bidrag.dokument.arkiv.query.TilknyttedeJournalposterQuery
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate

open class SafConsumer(private val restTemplate: RestTemplate) {
    fun hentDokument(journalpostId: Long, dokumentReferanse: Long): ResponseEntity<ByteArray> {
        return restTemplate.exchange(
            String.format("/rest/hentdokument/%s/%s/ARKIV", journalpostId, dokumentReferanse),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            ByteArray::class.java
        )
    }

    fun hentJournalpost(journalpostId: Long): Journalpost {
        return consumeEnkelJournalpostQuery(JournalpostQuery(journalpostId))
    }

    fun hentDistribusjonInfo(journalpostId: Long): DistribusjonsInfo {
        val response = consumeQuery(DistribusjonInfoQuery(journalpostId.toString())) { message: String? -> journalpostIkkeFunnetException(message) }
        return response.extractValueAsObject("journalpost", DistribusjonsInfo::class.java)
    }

    fun finnJournalposter(saksnummer: String, fagomrade: List<String> = listOf("BID")): List<Journalpost> {
        val response = consumeQuery(DokumentoversiktFagsakQuery(saksnummer, fagomrade)) { message: String? -> journalIkkeFunnetException(message) }
        return listOf(*response.extractValueAsObject("dokumentoversiktFagsak.journalposter", Array<Journalpost>::class.java))
    }

    fun finnTilknyttedeJournalposter(dokumentInfoId: String): List<TilknyttetJournalpost> {
        val response = consumeQuery(TilknyttedeJournalposterQuery(dokumentInfoId)) { message: String? -> journalIkkeFunnetException(message) }
        return listOf(*response.extractValueAsObject("tilknyttedeJournalposter", Array<TilknyttetJournalpost>::class.java))
    }

    private fun journalIkkeFunnetException(message: String?): RuntimeException {
        return JournalIkkeFunnetException(message ?: "")
    }

    private fun consumeEnkelJournalpostQuery(query: GraphQuery): Journalpost {
        val response = consumeQuery(query) { message: String? -> journalpostIkkeFunnetException(message) }
        return response.extractValueAsObject("journalpost", Journalpost::class.java)
    }

    private fun journalpostIkkeFunnetException(message: String?): RuntimeException {
        return JournalpostIkkeFunnetException(message ?: "")
    }

    private fun consumeQuery(query: GraphQuery, notFoundException: NotFoundException): GraphQLResponse {
        val queryString = query.getQuery()
        val graphQLClient = CustomGraphQLClient("") { _: String, _: Map<String, List<String>>, body: String ->
            val exchange = restTemplate.exchange("/graphql", HttpMethod.POST, HttpEntity(body), String::class.java)
            HttpResponse(exchange.statusCodeValue, exchange.body)
        }
        val response = graphQLClient.executeQuery(queryString, query.getVariables())
        if (response.hasErrors()) {
            val message = response.errors.stream()
                .findFirst()
                .map(GraphQLError::message)
                .orElseThrow()
            val errorReason = response.parsed.read<Any>("errors[0].extensions.code")
            val reasonToHttpStatus = ReasonToHttpStatus(errorReason)
            if (reasonToHttpStatus.status == HttpStatus.NOT_FOUND) {
                throw notFoundException.init(message)!!
            }
            throw SafException(
                String.format(
                    "Query %s med variabler (%s) feilet med feilmelding: %s",
                    query.javaClass.simpleName,
                    query.getVariables(),
                    message
                ),
                reasonToHttpStatus.status
            )
        }
        return response
    }

    private fun interface NotFoundException {
        fun init(message: String?): RuntimeException?
    }

    fun leggTilInterceptor(requestInterceptor: ClientHttpRequestInterceptor?) {
        (restTemplate as? HttpHeaderRestTemplate)?.interceptors?.add(requestInterceptor)
    }
}
