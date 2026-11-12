package it.davideangelone.mongopoc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/** Abilita @Retryable (Spring Framework 7) usato da UserService per i conflitti di concorrenza. */
@Configuration
@EnableResilientMethods
public class ResilienceConfig {
}
