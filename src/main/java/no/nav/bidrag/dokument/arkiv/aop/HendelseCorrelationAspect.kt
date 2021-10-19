package no.nav.bidrag.dokument.arkiv.aop

import no.nav.bidrag.commons.CorrelationId
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

@Component
@Aspect
class HendelseCorrelationAspect {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(HendelseCorrelationAspect::class.java)
        const val CORRELATION_ID = "correlationId"
    }

    @Before(value = "execution(* no.nav.bidrag.dokument.arkiv.kafka.HendelseListener.listen(..)) && args(hendelse)")
    fun addCorrelationIdToThread(joinPoint: JoinPoint, hendelse: JournalfoeringHendelseRecord) {
        LOGGER.info("Setting correlation id from hendelse")
        CorrelationId.existing("journalfoeringshendelse_"+hendelse.hendelsesId)
        MDC.put(CORRELATION_ID, hendelse.hendelsesId)
        RequestContextHolder.setRequestAttributes(KafkaRequestScopeAttributes())
    }

    @After(value = "execution(* no.nav.bidrag.dokument.arkiv.kafka.HendelseListener.listen(..))")
    fun clearCorrelationIdFromBehandleHendelseService(joinPoint: JoinPoint) {
        LOGGER.info("Resetting context")
        MDC.clear()
        RequestContextHolder.resetRequestAttributes()
    }
}
