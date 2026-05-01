package br.com.api.auroraorg.shared.config;

import br.com.api.auroraorg.shared.config.migration.V5_1__AddAttachmentEventTypes;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração complementar do Flyway para registrar Java-based migrations.
 *
 * Java migrations com canExecuteInTransaction() = false precisam ser
 * registradas explicitamente via javaMigrations() para que o Flyway
 * as execute fora de transação (necessário para ALTER TYPE no PostgreSQL).
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer(
            V5_1__AddAttachmentEventTypes addAttachmentEventTypes) {
        return config -> config.javaMigrations(addAttachmentEventTypes);
    }
}
