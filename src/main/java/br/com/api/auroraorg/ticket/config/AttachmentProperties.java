package br.com.api.auroraorg.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Propriedades de configuração para o módulo de anexos.
 *
 * Mapeadas de application.yml no prefixo app.attachments e app.storage.local.
 * Permite troca futura de storage (S3, MinIO) sem alterar lógica de negócio.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AttachmentProperties {

    private final Storage storage = new Storage();
    private final Attachments attachments = new Attachments();

    public Storage getStorage() {
        return storage;
    }

    public Attachments getAttachments() {
        return attachments;
    }

    public static class Storage {
        private final Local local = new Local();

        public Local getLocal() {
            return local;
        }

        public static class Local {
            private String basePath = "./storage";

            public String getBasePath() {
                return basePath;
            }

            public void setBasePath(String basePath) {
                this.basePath = basePath;
            }
        }
    }

    public static class Attachments {
        private long maxFileSizeBytes = 10L * 1024 * 1024; // 10 MB
        private int maxFilesPerTicket = 20;
        private List<String> allowedContentTypes = List.of(
                "image/png",
                "image/jpeg",
                "image/webp",
                "application/pdf",
                "text/plain",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public int getMaxFilesPerTicket() {
            return maxFilesPerTicket;
        }

        public void setMaxFilesPerTicket(int maxFilesPerTicket) {
            this.maxFilesPerTicket = maxFilesPerTicket;
        }

        public List<String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(List<String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }
    }
}
