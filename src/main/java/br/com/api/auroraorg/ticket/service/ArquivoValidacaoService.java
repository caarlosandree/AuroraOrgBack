package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.config.AttachmentProperties;
import br.com.api.auroraorg.ticket.exception.ArquivoInvalidoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Serviço de validação de segurança de arquivos recebidos via upload.
 *
 * Proteções implementadas:
 * - Arquivo vazio bloqueado
 * - Tamanho máximo por arquivo
 * - Content-Type validado contra allow-list
 * - Extensão validada contra deny-list de extensões perigosas
 * - Nome original sanitizado (sem path traversal)
 * - Ponto de extensão para análise antivírus futura
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArquivoValidacaoService {

    private static final Set<String> EXTENSOES_BLOQUEADAS = Set.of(
            "exe", "bat", "cmd", "sh", "bash", "ps1", "psm1",
            "js", "vbs", "jar", "war", "class", "php", "py",
            "rb", "pl", "cgi", "asp", "aspx", "jsp", "msi",
            "dll", "so", "dylib", "bin", "run", "com", "scr", "pif"
    );

    private final AttachmentProperties properties;

    /**
     * Valida todas as regras de segurança do arquivo.
     * Lança ArquivoInvalidoException com mensagem descritiva em caso de violação.
     */
    public void validar(MultipartFile arquivo) {
        validarNaoVazio(arquivo);
        validarTamanho(arquivo);
        validarContentType(arquivo);
        validarExtensao(arquivo);
        validarNomeOriginal(arquivo);
        // Ponto de extensão: chamar antivírus aqui no futuro
        // antivirusService.scan(arquivo);
    }

    // ========== VALIDAÇÕES ==========

    private void validarNaoVazio(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Arquivo vazio não é permitido.");
        }
    }

    private void validarTamanho(MultipartFile arquivo) {
        long maxBytes = properties.getAttachments().getMaxFileSizeBytes();
        if (arquivo.getSize() > maxBytes) {
            long maxMb = maxBytes / (1024 * 1024);
            throw new ArquivoInvalidoException(
                    "Arquivo excede o tamanho máximo permitido de " + maxMb + " MB. " +
                    "Tamanho recebido: " + (arquivo.getSize() / (1024 * 1024)) + " MB.");
        }
    }

    private void validarContentType(MultipartFile arquivo) {
        String contentType = arquivo.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new ArquivoInvalidoException("Content-Type ausente ou inválido.");
        }

        boolean permitido = properties.getAttachments().getAllowedContentTypes()
                .stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType.trim()));

        if (!permitido) {
            log.warn("Upload bloqueado: content-type não permitido '{}'", contentType);
            throw new ArquivoInvalidoException(
                    "Tipo de arquivo não permitido: " + contentType +
                    ". Tipos aceitos: imagens (PNG, JPEG, WEBP), PDF, TXT, DOC, DOCX, XLS, XLSX.");
        }
    }

    private void validarExtensao(MultipartFile arquivo) {
        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new ArquivoInvalidoException("Nome de arquivo ausente.");
        }

        String extensao = extrairExtensao(nomeOriginal).toLowerCase();
        if (extensao.isBlank()) {
            throw new ArquivoInvalidoException("Arquivo sem extensão não é permitido.");
        }

        if (EXTENSOES_BLOQUEADAS.contains(extensao)) {
            log.warn("Upload bloqueado: extensão perigosa '.{}' no arquivo '{}'", extensao, nomeOriginal);
            throw new ArquivoInvalidoException(
                    "Extensão de arquivo não permitida: ." + extensao +
                    ". Arquivos executáveis e scripts não são aceitos.");
        }
    }

    private void validarNomeOriginal(MultipartFile arquivo) {
        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new ArquivoInvalidoException("Nome de arquivo ausente.");
        }

        // Bloqueia tentativas de path traversal no nome
        if (nomeOriginal.contains("..") || nomeOriginal.contains("/") || nomeOriginal.contains("\\")) {
            log.warn("Upload bloqueado: tentativa de path traversal no nome '{}'", nomeOriginal);
            throw new ArquivoInvalidoException("Nome de arquivo inválido.");
        }

        if (nomeOriginal.length() > 255) {
            throw new ArquivoInvalidoException("Nome do arquivo muito longo. Máximo: 255 caracteres.");
        }
    }

    // ========== UTILITÁRIOS ==========

    /**
     * Extrai a extensão do nome do arquivo de forma segura.
     */
    public static String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal == null) return "";
        int dotIndex = nomeOriginal.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == nomeOriginal.length() - 1) return "";
        return nomeOriginal.substring(dotIndex + 1).toLowerCase();
    }
}
