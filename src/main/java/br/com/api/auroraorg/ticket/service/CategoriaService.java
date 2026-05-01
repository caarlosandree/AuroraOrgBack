package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Categoria;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.exception.CategoriaDuplicadaException;
import br.com.api.auroraorg.ticket.exception.CategoriaNotFoundException;
import br.com.api.auroraorg.ticket.exception.FilaNotFoundException;
import br.com.api.auroraorg.ticket.repository.CategoriaRepository;
import br.com.api.auroraorg.ticket.repository.FilaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final FilaRepository filaRepository;

    @Transactional
    public CategoriaResponse criarCategoria(CriarCategoriaRequest request) {
        if (categoriaRepository.existsByNameIgnoreCase(request.name())) {
            throw new CategoriaDuplicadaException(request.name());
        }

        Categoria categoria = Categoria.builder()
                .name(request.name())
                .description(request.description())
                .build();

        Categoria saved = categoriaRepository.save(categoria);
        log.info("Categoria criada: {} ({})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias(boolean apenasAtivas) {
        List<Categoria> categorias = apenasAtivas
                ? categoriaRepository.findAllByActiveTrueOrderByNameAsc()
                : categoriaRepository.findAll();
        return categorias.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarCategoria(UUID categoriaId) {
        Categoria categoria = findCategoriaOrThrow(categoriaId);
        return toResponse(categoria);
    }

    @Transactional(readOnly = true)
    public CategoriaDetalheResponse buscarCategoriaDetalhe(UUID categoriaId) {
        Categoria categoria = findCategoriaOrThrow(categoriaId);
        long totalChamados = categoriaRepository.countTicketsByCategoriaId(categoriaId);

        return new CategoriaDetalheResponse(
                categoria.getId(),
                categoria.getName(),
                categoria.getDescription(),
                categoria.getActive(),
                categoria.getFilaPadrao() != null ? toFilaResponse(categoria.getFilaPadrao()) : null,
                totalChamados,
                categoria.getCreatedAt(),
                categoria.getUpdatedAt()
        );
    }

    @Transactional
    public CategoriaResponse atualizarCategoria(UUID categoriaId, AtualizarCategoriaRequest request) {
        Categoria categoria = findCategoriaOrThrow(categoriaId);

        if (!categoria.getName().equalsIgnoreCase(request.name())
                && categoriaRepository.existsByNameIgnoreCase(request.name())) {
            throw new CategoriaDuplicadaException(request.name());
        }

        categoria.setName(request.name());
        categoria.setDescription(request.description());

        Categoria updated = categoriaRepository.save(categoria);
        log.info("Categoria atualizada: {} ({})", updated.getName(), updated.getId());
        return toResponse(updated);
    }

    @Transactional
    public CategoriaResponse ativarCategoria(UUID categoriaId) {
        Categoria categoria = findCategoriaOrThrow(categoriaId);
        categoria.setActive(true);
        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse desativarCategoria(UUID categoriaId) {
        Categoria categoria = findCategoriaOrThrow(categoriaId);
        categoria.setActive(false);
        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse definirFilaPadrao(UUID categoriaId, DefinirFilaPadraoCategoriaRequest request) {
        Categoria categoria = findCategoriaOrThrow(categoriaId);
        Fila fila = filaRepository.findById(request.filaPadraoId())
                .orElseThrow(() -> new FilaNotFoundException(request.filaPadraoId()));

        categoria.setFilaPadrao(fila);
        Categoria updated = categoriaRepository.save(categoria);
        log.info("Fila padrão {} definida para categoria {}", fila.getName(), categoria.getName());
        return toResponse(updated);
    }

    private Categoria findCategoriaOrThrow(UUID categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new CategoriaNotFoundException(categoriaId));
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getName(),
                categoria.getDescription(),
                categoria.getActive(),
                categoria.getFilaPadrao() != null ? toFilaResponse(categoria.getFilaPadrao()) : null,
                categoria.getCreatedAt(),
                categoria.getUpdatedAt()
        );
    }

    private FilaResponse toFilaResponse(Fila fila) {
        return new FilaResponse(
                fila.getId(),
                fila.getName(),
                fila.getDescription(),
                fila.getActive(),
                fila.getCreatedAt(),
                fila.getUpdatedAt()
        );
    }
}
