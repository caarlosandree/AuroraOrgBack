package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Categoria;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.entity.SlaPolitica;
import br.com.api.auroraorg.ticket.exception.SlaPoliticaInvalidaException;
import br.com.api.auroraorg.ticket.exception.SlaPoliticaNaoEncontradaException;
import br.com.api.auroraorg.ticket.repository.CategoriaRepository;
import br.com.api.auroraorg.ticket.repository.FilaRepository;
import br.com.api.auroraorg.ticket.repository.SlaPoliticaRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de Políticas Configuráveis de SLA.
 *
 * No MVP: implementa CRUD básico de políticas.
 * Futuro: suporta políticas por fila, categoria e cliente.
 *
 * Permissões:
 * - ADMIN: cria, edita, ativa/desativa qualquer política
 * - GESTOR: visualiza (não altera, salvo regra explícita)
 * - AGENTE/SOLICITANTE: apenas visualiza políticas ativas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaPoliticaService {

    private final SlaPoliticaRepository slaPoliticaRepository;
    private final FilaRepository filaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public SlaPoliticaResponse criar(CriarSlaPoliticaRequest request) {
        securityUtils.isAdmin(); // Apenas ADMIN

        log.info("Criando política de SLA: {}", request.nome());

        validarRequest(request);

        SlaPolitica politica = SlaPolitica.builder()
                .nome(request.nome().trim())
                .prioridade(request.prioridade())
                .prazoPrimeiraRespostaMinutos(request.prazoPrimeiraRespostaMinutos())
                .prazoResolucaoMinutos(request.prazoResolucaoMinutos())
                .fila(resolveFila(request.filaId()))
                .categoria(resolveCategoria(request.categoriaId()))
                .ativa(true)
                .build();

        SlaPolitica saved = slaPoliticaRepository.save(politica);
        log.info("Política de SLA criada: {} (id={})", saved.getNome(), saved.getId());

        return toResponse(saved);
    }

    @Transactional
    public SlaPoliticaResponse atualizar(UUID id, AtualizarSlaPoliticaRequest request) {
        securityUtils.isAdmin();

        log.info("Atualizando política de SLA: {}", id);

        SlaPolitica politica = slaPoliticaRepository.findById(id)
                .orElseThrow(() -> new SlaPoliticaNaoEncontradaException(id));

        if (request.nome() != null) {
            politica.setNome(request.nome().trim());
        }
        if (request.prazoPrimeiraRespostaMinutos() != null) {
            politica.setPrazoPrimeiraRespostaMinutos(request.prazoPrimeiraRespostaMinutos());
        }
        if (request.prazoResolucaoMinutos() != null) {
            politica.setPrazoResolucaoMinutos(request.prazoResolucaoMinutos());
        }
        if (request.filaId() != null) {
            politica.setFila(resolveFila(request.filaId()));
        }
        if (request.categoriaId() != null) {
            politica.setCategoria(resolveCategoria(request.categoriaId()));
        }

        SlaPolitica updated = slaPoliticaRepository.save(politica);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public SlaPoliticaResponse buscar(UUID id) {
        SlaPolitica politica = slaPoliticaRepository.findById(id)
                .orElseThrow(() -> new SlaPoliticaNaoEncontradaException(id));
        return toResponse(politica);
    }

    @Transactional(readOnly = true)
    public List<SlaPoliticaResponse> listar() {
        return slaPoliticaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SlaPoliticaResponse> listarAtivas() {
        return slaPoliticaRepository.findByAtivaTrueOrderByPrioridadeAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void ativar(UUID id) {
        securityUtils.isAdmin();
        SlaPolitica politica = slaPoliticaRepository.findById(id)
                .orElseThrow(() -> new SlaPoliticaNaoEncontradaException(id));
        politica.setAtiva(true);
        slaPoliticaRepository.save(politica);
        log.info("Política de SLA ativada: {}", id);
    }

    @Transactional
    public void desativar(UUID id) {
        securityUtils.isAdmin();
        SlaPolitica politica = slaPoliticaRepository.findById(id)
                .orElseThrow(() -> new SlaPoliticaNaoEncontradaException(id));
        politica.setAtiva(false);
        slaPoliticaRepository.save(politica);
        log.info("Política de SLA desativada: {}", id);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void validarRequest(CriarSlaPoliticaRequest request) {
        if (request.prazoResolucaoMinutos() <= request.prazoPrimeiraRespostaMinutos()) {
            throw new SlaPoliticaInvalidaException(
                    "O prazo de resolução deve ser maior que o prazo de primeira resposta.");
        }
    }

    private Fila resolveFila(UUID filaId) {
        if (filaId == null) {
            return null;
        }
        return filaRepository.findById(filaId)
                .orElseThrow(() -> new SlaPoliticaInvalidaException("Fila não encontrada: " + filaId));
    }

    private Categoria resolveCategoria(UUID categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new SlaPoliticaInvalidaException("Categoria não encontrada: " + categoriaId));
    }

    private SlaPoliticaResponse toResponse(SlaPolitica p) {
        return new SlaPoliticaResponse(
                p.getId(),
                p.getNome(),
                p.getPrioridade(),
                p.getPrioridade().getLabel(),
                p.getPrazoPrimeiraRespostaMinutos(),
                p.getPrazoResolucaoMinutos(),
                p.getAtiva(),
                p.getFila() != null ? p.getFila().getId() : null,
                p.getFila() != null ? p.getFila().getName() : null,
                p.getCategoria() != null ? p.getCategoria().getId() : null,
                p.getCategoria() != null ? p.getCategoria().getName() : null,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
