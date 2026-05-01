package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.FilaAgenteResponse;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.entity.FilaAgente;
import br.com.api.auroraorg.ticket.entity.FilaAgenteId;
import br.com.api.auroraorg.ticket.exception.*;
import br.com.api.auroraorg.ticket.repository.FilaAgenteRepository;
import br.com.api.auroraorg.ticket.repository.FilaRepository;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilaAgenteService {

    private final FilaAgenteRepository filaAgenteRepository;
    private final FilaRepository filaRepository;
    private final UserRepository userRepository;

    @Transactional
    public void adicionarAgente(UUID filaId, UUID agenteId) {
        Fila fila = filaRepository.findById(filaId)
                .orElseThrow(() -> new FilaNotFoundException(filaId));

        User agente = userRepository.findById(agenteId)
                .orElseThrow(() -> new br.com.api.auroraorg.shared.exception.ResourceNotFoundException("Usuário", "id", agenteId));

        if (agente.getRole() != UserRole.AGENTE && agente.getRole() != UserRole.ADMIN) {
            throw new AgenteNaoPermitidoException(agenteId);
        }

        if (filaAgenteRepository.existsByFilaIdAndAgenteId(filaId, agenteId)) {
            throw new AgenteJaVinculadoException();
        }

        FilaAgente filaAgente = FilaAgente.builder()
                .id(new FilaAgenteId(filaId, agenteId))
                .fila(fila)
                .agente(agente)
                .build();

        filaAgenteRepository.save(filaAgente);
        log.info("Agente {} adicionado à fila {}", agente.getEmail(), fila.getName());
    }

    @Transactional
    public void removerAgente(UUID filaId, UUID agenteId) {
        if (!filaRepository.existsById(filaId)) {
            throw new FilaNotFoundException(filaId);
        }

        if (!filaAgenteRepository.existsByFilaIdAndAgenteId(filaId, agenteId)) {
            throw new AgenteNaoVinculadoException();
        }

        filaAgenteRepository.deleteById(new FilaAgenteId(filaId, agenteId));
        log.info("Agente {} removido da fila {}", agenteId, filaId);
    }

    @Transactional(readOnly = true)
    public List<FilaAgenteResponse> listarAgentesDaFila(UUID filaId) {
        if (!filaRepository.existsById(filaId)) {
            throw new FilaNotFoundException(filaId);
        }

        return filaAgenteRepository.findAllByFilaIdWithAgente(filaId).stream()
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
    }
}
