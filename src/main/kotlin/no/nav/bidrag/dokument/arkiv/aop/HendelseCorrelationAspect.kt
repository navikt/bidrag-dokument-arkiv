package no.nav.bidrag.dokument.arkiv.aop

import no.nav.bidrag.commons.CorrelationId
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

@Component
@Aspect
class HendelseCorrelationAspect {

    companion object {
        const val CORRELATION_ID = "correlationId"
    }

    @Before(value = "execution(* no.nav.bidrag.dokument.arkiv.kafka.HendelseListener.listenJournalforingHendelse(..)) && args(hendelse)")
    fun addCorrelationIdToThreadJournalforingHendelse(joinPoint: JoinPoint, hendelse: JournalfoeringHendelseRecord) {
        val correlationId = CorrelationId.generateTimestamped("journalfoeringshendelse_" + hendelse.hendelsesId)
        MDC.put(CORRELATION_ID, correlationId.get())
        RequestContextHolder.setRequestAttributes(KafkaRequestScopeAttributes())
    }

    @Before(value = "execution(* no.nav.bidrag.dokument.arkiv.kafka.HendelseListener.lesOppgaveOpprettetHendelse(..)) && args(hendelse)")
    fun addCorrelationIdToThreadOppgaveOpprettetHendelse(joinPoint: JoinPoint, hendelse: ConsumerRecord<String, String>) {
        val correlationId = CorrelationId.generateTimestamped("oppgaveOpprettet")
        MDC.put(CORRELATION_ID, correlationId.get())
        RequestContextHolder.setRequestAttributes(KafkaRequestScopeAttributes())
    }

    @After(value = "execution(* no.nav.bidrag.dokument.arkiv.kafka.HendelseListener.*(..))")
    fun clearCorrelationIdFromBehandleHendelseService(joinPoint: JoinPoint) {
        MDC.clear()
        RequestContextHolder.resetRequestAttributes()
    }
}
