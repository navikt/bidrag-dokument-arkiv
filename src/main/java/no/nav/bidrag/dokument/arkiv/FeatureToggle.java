package no.nav.bidrag.dokument.arkiv;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeatureToggle {
  public enum Feature {
    KAFKA_ARBEIDSFLYT
  }
  @Value("${FEATURE_ENABLED}")
  private String featureEnabled;

  public boolean isFeatureEnabled(Feature feature){
    return Optional.ofNullable(featureEnabled).orElse("").matches(feature.name());
  }
}
