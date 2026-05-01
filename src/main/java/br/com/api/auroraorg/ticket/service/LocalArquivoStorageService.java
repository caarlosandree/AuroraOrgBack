package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.config.AttachmentProperties;
import br.com.api.auroraorg.ticket.exception.AnexoStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implementação de storage em disco local para o MVP.
 *
 * Estrutura de diretórios: {basePath}/chamados/{ticketId}/anexos/{fileName}
 *
 * Para migrar para S3/MinIO/Azure Blob:
 * 1. Crie S3ArquivoStorageService implementando ArquivoStorageService
 * 2. Anote com @Primary ou use perfil Spring (@Profile("prod"))
 * 3. Nenhuma outra classe precisa mudar.
 *
 * Segurança implementada:
 * - Nunca usa nome original do cliente no path
 * - Valida que o path resolvido está dentro do basePath (anti path traversal)
 * - Cria diretórios intermediários automaticamente
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalArquivoStorageService implements ArquivoStorageService {

    private final AttachmentProperties properties;

    @Override
    public String salvar(MultipartFile arquivo, UUID ticketId, String fileName) {
        String relativePath = "chamados/" + ticketId + "/anexos/" + fileName;
        Path destino = resolverPath(relativePath);

        garantirDiretorio(destino.getParent());
        validarPathSeguro(destino);

        try {
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            log.info("Arquivo salvo em disco: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            throw new AnexoStorageException("Falha ao salvar arquivo no disco: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource carregar(String storagePath) {
        Path caminho = resolverPath(storagePath);
        validarPathSeguro(caminho);

        try {
            Resource resource = new UrlResource(caminho.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new AnexoStorageException("Arquivo não encontrado ou ilegível: " + storagePath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new AnexoStorageException("Caminho inválido: " + storagePath, e);
        }
    }

    @Override
    public boolean existe(String storagePath) {
        return Files.exists(resolverPath(storagePath));
    }

    @Override
    public void deletar(String storagePath) {
        // No MVP: preservamos o arquivo físico para auditoria.
        // Futuramente: Files.deleteIfExists(resolverPath(storagePath));
        log.debug("Deleção física não executada (preservada para auditoria): {}", storagePath);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private Path resolverPath(String relativePath) {
        Path base = Paths.get(properties.getStorage().getLocal().getBasePath()).toAbsolutePath().normalize();
        return base.resolve(relativePath).normalize();
    }

    /**
     * Garante que o path resolvido está dentro do basePath configurado.
     * Proteção contra Path Traversal (ex: ../../etc/passwd).
     */
    private void validarPathSeguro(Path destino) {
        Path base = Paths.get(properties.getStorage().getLocal().getBasePath()).toAbsolutePath().normalize();
        if (!destino.startsWith(base)) {
            throw new AnexoStorageException("Tentativa de acesso fora do diretório de storage. Path rejeitado.");
        }
    }

    private void garantirDiretorio(Path diretorio) {
        try {
            Files.createDirectories(diretorio);
        } catch (IOException e) {
            throw new AnexoStorageException("Não foi possível criar diretório de storage: " + e.getMessage(), e);
        }
    }
}
