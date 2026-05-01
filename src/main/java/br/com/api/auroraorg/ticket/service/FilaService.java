package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.exception.FilaDuplicadaException;
import br.com.api.auroraorg.ticket.exception.FilaNotFoundException;
import br.com.api.auroraorg.ticket.repository.FilaAgenteRepository;
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
public class FilaService {

    private final FilaRepository filaRepository;
    private final FilaAgenteRepository filaAgenteRepository;

    @Transactional
    public FilaResponse criarFila(CriarFilaRequest request) {
        if (filaRepository.existsByNameIgnoreCase(request.name())) {
            throw new FilaDuplicadaException(request.name());
        }

        Fila fila = Fila.builder()
                .name(request.name())
                .description(request.description())
                .build();

        Fila saved = filaRepository.save(fila);
        log.info("Fila criada: {} ({})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FilaResponse> listarFilas(boolean apenasAtivas) {
        List<Fila> filas = apenasAtivas
                ? filaRepository.findAllByActiveTrueOrderByNameAsc()
                : filaRepository.findAll();
        return filas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FilaResponse buscarFila(UUID filaId) {
        Fila fila = findFilaOrThrow(filaId);
        return toResponse(fila);
    }

    @Transactional(readOnly = true)
    public FilaDetalheResponse buscarFilaComAgentes(UUID filaId) {
        Fila fila = findFilaOrThrow(filaId);
        List<FilaAgenteResponse> agentes = filaAgenteRepository.findAllByFilaIdWithAgente(filaId).stream()
                .map(fa -> new FilaAgenteResponse(
                        new br.com.api.auroraorg.user.dto.UserResponse(
                                fa.getAgente().getId(),
                                fa.getAgente().getName(),
                                fa.getAgente().getEmail(),
                                fa.getAgente().getRole(),
                                fa.getAgente().getActive(),
                                fa.getAgente().getCreatedAt(),
                                fa.getAgente().getUpdatedAt()
                        ),
                        fa.getCreatedAt()
                ))
                .toList();

        return new FilaDetalheResponse(
                fila.getId(),
                fila.getName(),
                fila.getDescription(),
                fila.getActive(),
                agentes,
                fila.getCreatedAt(),
                fila.getUpdatedAt()
        );
    }

    @Transactional
    public FilaResponse atualizarFila(UUID filaId, AtualizarFilaRequest request) {
        Fila fila = findFilaOrThrow(filaId);

        if (!fila.getName().equalsIgnoreCase(request.name())
                && filaRepository.existsByNameIgnoreCase(request.name())) {
            throw new FilaDuplicadaException(request.name());
        }

        fila.setName(request.name());
        fila.setDescription(request.description());

        Fila updated = filaRepository.save(fila);
        log.info("Fila atualizada: {} ({})", updated.getName(), updated.getId());
        return toResponse(updated);
    }

    @Transactional
    public FilaResponse ativarFila(UUID filaId) {
        Fila fila = findFilaOrThrow(filaId);
        fila.setActive(true);
        return toResponse(filaRepository.save(fila));
    }

    @Transactional
    public FilaResponse desativarFila(UUID filaId) {
        Fila fila = findFilaOrThrow(filaId);
        fila.setActive(false);
        return toResponse(filaRepository.save(fila));
    }

    private Fila findFilaOrThrow(UUID filaId) {
        return filaRepository.findById(filaId)
                .orElseThrow(() -> new FilaNotFoundException(filaId));
    }

    private FilaResponse toResponse(Fila fila) {
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
