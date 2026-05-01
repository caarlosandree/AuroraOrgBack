package br.com.api.auroraorg.shared.config.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.Statement;

/**
 * Migration Java para adicionar novos valores ao enum event_type.
 *
 * Por que Java Migration e não SQL?
 * ALTER TYPE ... ADD VALUE não pode ser executado dentro de uma transação
 * no PostgreSQL (até a versão 18). O Flyway encapsula SQL migrations em
 * transações por padrão. Esta classe estende BaseJavaMigration e sobrescreve
 * canExecuteInTransaction() retornando false, o que instrui o Flyway a
 * executar esta migration fora de qualquer transação.
 */
@Component
public class V5_1__AddAttachmentEventTypes extends BaseJavaMigration {

    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement st = context.getConnection().createStatement()) {
            st.execute("ALTER TYPE event_type ADD VALUE IF NOT EXISTS 'ANEXO_ADICIONADO'");
            st.execute("ALTER TYPE event_type ADD VALUE IF NOT EXISTS 'ANEXO_REMOVIDO'");
        }
    }
}
