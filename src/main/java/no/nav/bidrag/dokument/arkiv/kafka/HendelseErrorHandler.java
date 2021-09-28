package no.nav.bidrag.dokument.arkiv.kafka;

import no.nav.bidrag.commons.ExceptionLogger;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;

public class HendelseErrorHandler implements KafkaListenerErrorHandler {

  private final ExceptionLogger exceptionLogger;

  HendelseErrorHandler(ExceptionLogger exceptionLogger) {
    this.exceptionLogger = exceptionLogger;
  }

  @Override
  public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
    exceptionLogger.logException(exception.getCause(), HendelseErrorHandler.class.getSimpleName());

    return null;
  }
}
