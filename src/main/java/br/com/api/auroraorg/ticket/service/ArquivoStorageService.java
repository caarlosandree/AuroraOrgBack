package br.com.api.auroraorg.ticket.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Contrato de armazenamento de arquivos.
 *
 * Abstrai o backend de storage (disco local no MVP; S3, MinIO ou Azure Blob futuramente).
 * A troca de implementação exige apenas um novo @Primary ou perfil Spring,
 * sem tocar em nenhuma outra classe.
 */
public interface ArquivoStorageService {

    /**
     * Salva o arquivo e retorna o caminho relativo onde foi armazenado.
     *
     * @param arquivo   arquivo recebido via multipart
     * @param ticketId  ID do chamado — usado para organizar o diretório
     * @param fileName  nome gerado com segurança para armazenamento
     * @return caminho relativo (storage_path) a ser persistido no banco
     */
    String salvar(MultipartFile arquivo, UUID ticketId, String fileName);

    /**
     * Carrega o arquivo como Resource para streaming na resposta HTTP.
     *
     * @param storagePath caminho relativo retornado por {@link #salvar}
     * @return Resource pronto para ser escrito na resposta
     */
    Resource carregar(String storagePath);

    /**
     * Verifica se um arquivo existe no storage.
     *
     * @param storagePath caminho relativo
     * @return true se o arquivo existir
     */
    boolean existe(String storagePath);

    /**
     * Ponto de extensão para futura deleção física.
     * No MVP com disco local, pode ser no-op (preservamos por auditoria).
     *
     * @param storagePath caminho relativo
     */
    void deletar(String storagePath);
}
