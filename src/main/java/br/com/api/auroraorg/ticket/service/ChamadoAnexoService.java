package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.config.AttachmentProperties;
import br.com.api.auroraorg.ticket.dto.AnexoDetalheResponse;
import br.com.api.auroraorg.ticket.dto.AnexoResponse;
import br.com.api.auroraorg.ticket.dto.UploadAnexoResponse;
import br.com.api.auroraorg.ticket.entity.ChamadoAnexo;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketEventOrigin;
import br.com.api.auroraorg.ticket.enums.TicketEventType;
import br.com.api.auroraorg.ticket.exception.*;
import br.com.api.auroraorg.ticket.repository.ChamadoAnexoRepository;
import br.com.api.auroraorg.ticket.repository.TicketEventRepository;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.entity.TicketEvent;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço principal do Módulo 4 — Anexos de Chamados.
 *
 * Responsabilidades:
 * - Orquestrar upload, listagem, download e remoção lógica de anexos
 * - Validar permissões por perfil (ADMIN, AGENTE, SOLICITANTE, GESTOR)
 * - Registrar eventos de auditoria na timeline do chamado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChamadoAnexoService {

    private final ChamadoAnexoRepository anexoRepository;
    private final TicketRepository ticketRepository;
    private final TicketEventRepository eventRepository;
    private final ArquivoStorageService storageService;
    private final ArquivoValidacaoService validacaoService;
    private final SecurityUtils securityUtils;
    private final AttachmentProperties properties;

    // ========== UPLOAD ==========

    /**
     * Faz upload de um novo anexo ao chamado.
     *
     * Regras:
     * - SOLICITANTE: somente no próprio chamado, status não terminal
     * - AGENTE: chamados acessíveis, status não terminal
     * - ADMIN: qualquer chamado, inclusive FECHADO/CANCELADO
     * - GESTOR: por padrão, não pode anexar (regra explícita)
     */
    @Transactional
    public UploadAnexoResponse upload(UUID ticketId, MultipartFile arquivo) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = buscarChamadoOuErro(ticketId);

        validarPermissaoUpload(ticket, currentUser);
        validarStatusParaUpload(ticket, currentUser);
        validarLimiteAnexos(ticketId);
        validacaoService.validar(arquivo);

        String nomeGerado = gerarNomeArquivo(arquivo.getOriginalFilename());
        String storagePath = storageService.salvar(arquivo, ticketId, nomeGerado);

        ChamadoAnexo anexo = ChamadoAnexo.builder()
                .ticket(ticket)
                .fileName(nomeGerado)
                .originalFileName(arquivo.getOriginalFilename())
                .contentType(arquivo.getContentType())
                .fileSize(arquivo.getSize())
                .storagePath(storagePath)
                .uploadedBy(currentUser)
                .build();

        ChamadoAnexo salvo = anexoRepository.save(anexo);
        registrarEventoAnexoAdicionado(ticket, currentUser, salvo);

        log.info("Anexo {} adicionado ao chamado {} por {}", salvo.getId(), ticketId, currentUser.getEmail());

        return new UploadAnexoResponse(
                salvo.getId(),
                ticketId,
                salvo.getOriginalFileName(),
                salvo.getContentType(),
                salvo.getFileSize(),
                salvo.getCreatedAt(),
                "Arquivo anexado com sucesso."
        );
    }

    // ========== LISTAGEM ==========

    /**
     * Lista anexos ativos de um chamado.
     * SOLICITANTE vê apenas os do próprio chamado; AGENTE/ADMIN/GESTOR veem todos.
     */
    @Transactional(readOnly = true)
    public List<AnexoResponse> listar(UUID ticketId) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = buscarChamadoOuErro(ticketId);

        validarPermissaoLeitura(ticket, currentUser);

        return anexoRepository.findActiveByTicketId(ticketId).stream()
                .map(this::toAnexoResponse)
                .collect(Collectors.toList());
    }

    // ========== DETALHE ==========

    @Transactional(readOnly = true)
    public AnexoDetalheResponse detalhe(UUID ticketId, UUID anexoId) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = buscarChamadoOuErro(ticketId);

        validarPermissaoLeitura(ticket, currentUser);

        ChamadoAnexo anexo = buscarAnexoAtivoOuErro(anexoId, ticketId);
        return toAnexoDetalheResponse(anexo);
    }

    // ========== DOWNLOAD ==========

    /**
     * Carrega o arquivo para download.
     * Retorna o Resource; o Controller configura os headers.
     */
    @Transactional(readOnly = true)
    public Resource download(UUID ticketId, UUID anexoId) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = buscarChamadoOuErro(ticketId);

        validarPermissaoLeitura(ticket, currentUser);

        ChamadoAnexo anexo = buscarAnexoAtivoOuErro(anexoId, ticketId);

        log.info("Download do anexo {} por {}", anexoId, currentUser.getEmail());
        return storageService.carregar(anexo.getStoragePath());
    }

    /**
     * Retorna o nome original seguro para uso no Content-Disposition.
     */
    @Transactional(readOnly = true)
    public String nomeOriginalParaDownload(UUID ticketId, UUID anexoId) {
        return buscarAnexoAtivoOuErro(anexoId, ticketId).getOriginalFileName();
    }

    // ========== REMOÇÃO LÓGICA ==========

    /**
     * Remove logicamente o anexo.
     *
     * Regras:
     * - ADMIN: pode remover qualquer anexo
     * - AGENTE: pode remover os próprios; remover de outros somente se policy permitir (aqui: bloqueado)
     * - SOLICITANTE: somente os próprios e se o chamado não for FECHADO/CANCELADO
     * - GESTOR: não remove (por padrão)
     */
    @Transactional
    public void remover(UUID ticketId, UUID anexoId) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = buscarChamadoOuErro(ticketId);
        ChamadoAnexo anexo = buscarAnexoAtivoOuErro(anexoId, ticketId);

        validarPermissaoRemocao(ticket, anexo, currentUser);

        anexo.removerLogicamente(currentUser);
        anexoRepository.save(anexo);

        registrarEventoAnexoRemovido(ticket, currentUser, anexo);

        log.info("Anexo {} removido logicamente do chamado {} por {}", anexoId, ticketId, currentUser.getEmail());
    }

    // ========== VALIDAÇÕES DE PERMISSÃO ==========

    private void validarPermissaoUpload(Ticket ticket, User user) {
        UserRole role = user.getRole();

        switch (role) {
            case ADMIN -> { /* sempre pode */ }
            case AGENTE -> { /* pode em chamados acessíveis — simplificado: qualquer chamado aberto */ }
            case SOLICITANTE -> {
                if (!ticket.isRequestedBy(user)) {
                    throw new AnexoPermissaoNegadaException("enviar anexo",
                            "Solicitante só pode anexar arquivos no próprio chamado.");
                }
            }
            case GESTOR -> throw new AnexoPermissaoNegadaException("enviar anexo",
                    "GESTOR não possui permissão padrão para enviar anexos.");
            default -> throw new AnexoPermissaoNegadaException("enviar anexo");
        }
    }

    private void validarStatusParaUpload(Ticket ticket, User user) {
        if (ticket.getStatus().isTerminal() && user.getRole() != UserRole.ADMIN) {
            throw new ChamadoNaoPermiteAnexoException(ticket.getStatus());
        }
    }

    private void validarPermissaoLeitura(Ticket ticket, User user) {
        UserRole role = user.getRole();

        if (role == UserRole.SOLICITANTE && !ticket.isRequestedBy(user)) {
            throw new AnexoPermissaoNegadaException("listar anexos",
                    "Solicitante só pode ver anexos do próprio chamado.");
        }
    }

    private void validarPermissaoRemocao(Ticket ticket, ChamadoAnexo anexo, User user) {
        UserRole role = user.getRole();

        switch (role) {
            case ADMIN -> { /* sempre pode */ }
            case AGENTE -> {
                boolean ehProprioAnexo = anexo.getUploadedBy().getId().equals(user.getId());
                if (!ehProprioAnexo) {
                    throw new AnexoPermissaoNegadaException("remover anexo",
                            "AGENTE só pode remover os próprios anexos.");
                }
            }
            case SOLICITANTE -> {
                if (!ticket.isRequestedBy(user)) {
                    throw new AnexoPermissaoNegadaException("remover anexo",
                            "Solicitante só pode remover anexos do próprio chamado.");
                }
                if (!anexo.getUploadedBy().getId().equals(user.getId())) {
                    throw new AnexoPermissaoNegadaException("remover anexo",
                            "Solicitante só pode remover os próprios anexos.");
                }
                if (ticket.getStatus().isTerminal()) {
                    throw new AnexoPermissaoNegadaException("remover anexo",
                            "Chamado já está encerrado. Remoção de anexos não é permitida.");
                }
            }
            case GESTOR -> throw new AnexoPermissaoNegadaException("remover anexo",
                    "GESTOR não possui permissão padrão para remover anexos.");
            default -> throw new AnexoPermissaoNegadaException("remover anexo");
        }
    }

    // ========== VALIDAÇÕES DE NEGÓCIO ==========

    private void validarLimiteAnexos(UUID ticketId) {
        int limite = properties.getAttachments().getMaxFilesPerTicket();
        long atual = anexoRepository.countActiveByTicketId(ticketId);
        if (atual >= limite) {
            throw new LimiteAnexosExcedidoException(limite);
        }
    }

    // ========== REGISTRO DE EVENTOS NA TIMELINE ==========

    private void registrarEventoAnexoAdicionado(Ticket ticket, User actor, ChamadoAnexo anexo) {
        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.ANEXO_ADICIONADO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(actor.getName() + " anexou o arquivo " + anexo.getOriginalFileName())
                .description("Arquivo " + anexo.getOriginalFileName() + " adicionado ao chamado")
                .metadata(Map.of(
                        "attachmentId", anexo.getId().toString(),
                        "fileName", anexo.getOriginalFileName(),
                        "contentType", anexo.getContentType(),
                        "fileSize", anexo.getFileSize()
                ))
                .visibility(CommentVisibility.PUBLICO)
                .build();

        eventRepository.save(event);
    }

    private void registrarEventoAnexoRemovido(Ticket ticket, User actor, ChamadoAnexo anexo) {
        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.ANEXO_REMOVIDO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(actor.getName() + " removeu o anexo " + anexo.getOriginalFileName())
                .description("Anexo " + anexo.getOriginalFileName() + " removido logicamente")
                .metadata(Map.of(
                        "attachmentId", anexo.getId().toString(),
                        "fileName", anexo.getOriginalFileName(),
                        "contentType", anexo.getContentType(),
                        "fileSize", anexo.getFileSize()
                ))
                .visibility(CommentVisibility.INTERNO)
                .build();

        eventRepository.save(event);
    }

    // ========== BUSCAS INTERNAS ==========

    private Ticket buscarChamadoOuErro(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private ChamadoAnexo buscarAnexoAtivoOuErro(UUID anexoId, UUID ticketId) {
        return anexoRepository.findActiveByIdAndTicketId(anexoId, ticketId)
                .orElseThrow(() -> new AnexoNotFoundException(anexoId, ticketId));
    }

    // ========== MAPEAMENTO ==========

    private AnexoResponse toAnexoResponse(ChamadoAnexo a) {
        return new AnexoResponse(
                a.getId(),
                a.getTicket().getId(),
                a.getOriginalFileName(),
                a.getContentType(),
                a.getFileSize(),
                new AnexoResponse.UploadedByInfo(a.getUploadedBy().getId(), a.getUploadedBy().getName()),
                a.getCreatedAt()
        );
    }

    private AnexoDetalheResponse toAnexoDetalheResponse(ChamadoAnexo a) {
        AnexoDetalheResponse.RemovedByInfo removedByInfo = a.getRemovedBy() != null
                ? new AnexoDetalheResponse.RemovedByInfo(a.getRemovedBy().getId(), a.getRemovedBy().getName())
                : null;

        return new AnexoDetalheResponse(
                a.getId(),
                a.getTicket().getId(),
                a.getOriginalFileName(),
                a.getContentType(),
                a.getFileSize(),
                new AnexoResponse.UploadedByInfo(a.getUploadedBy().getId(), a.getUploadedBy().getName()),
                a.getCreatedAt(),
                a.isRemoved(),
                a.getRemovedAt(),
                removedByInfo
        );
    }

    // ========== GERAÇÃO DE NOME SEGURO ==========

    private String gerarNomeArquivo(String nomeOriginal) {
        String extensao = ArquivoValidacaoService.extrairExtensao(nomeOriginal);
        String sufixo = extensao.isBlank() ? "" : "." + extensao;
        return UUID.randomUUID() + sufixo;
    }
}
