package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.exception.FilaDuplicadaException;
import br.com.api.auroraorg.ticket.exception.FilaNotFoundException;
import br.com.api.auroraorg.ticket.repository.FilaAgenteRepository;
import br.com.api.auroraorg.ticket.repository.FilaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilaServiceTest {

    @Mock
    private FilaRepository filaRepository;

    @Mock
    private FilaAgenteRepository filaAgenteRepository;

    @InjectMocks
    private FilaService filaService;

    private Fila fila;

    @BeforeEach
    void setUp() {
        fila = createFila(UUID.randomUUID(), "Suporte Técnico", "Atendimento técnico");
    }

    @Test
    @DisplayName("Deve criar fila com sucesso")
    void shouldCreateFila() {
        when(filaRepository.existsByNameIgnoreCase("Suporte Técnico")).thenReturn(false);
        when(filaRepository.save(any(Fila.class))).thenReturn(fila);

        CriarFilaRequest request = new CriarFilaRequest("Suporte Técnico", "Atendimento técnico");
        FilaResponse response = filaService.criarFila(request);

        assertThat(response.name()).isEqualTo("Suporte Técnico");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar fila com nome duplicado")
    void shouldThrowWhenCreatingDuplicateFila() {
        when(filaRepository.existsByNameIgnoreCase("Suporte Técnico")).thenReturn(true);

        CriarFilaRequest request = new CriarFilaRequest("Suporte Técnico", "Atendimento técnico");

        assertThatThrownBy(() -> filaService.criarFila(request))
                .isInstanceOf(FilaDuplicadaException.class)
                .hasMessageContaining("Suporte Técnico");
    }

    @Test
    @DisplayName("Deve listar apenas filas ativas")
    void shouldListActiveFilas() {
        Fila filaInativa = createFila(UUID.randomUUID(), "Legado", "Fila antiga");
        filaInativa.setActive(false);

        when(filaRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(fila));

        List<FilaResponse> result = filaService.listarFilas(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Suporte Técnico");
    }

    @Test
    @DisplayName("Deve buscar fila por ID")
    void shouldFindFilaById() {
        when(filaRepository.findById(fila.getId())).thenReturn(Optional.of(fila));

        FilaResponse response = filaService.buscarFila(fila.getId());

        assertThat(response.id()).isEqualTo(fila.getId());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar fila inexistente")
    void shouldThrowWhenFilaNotFound() {
        UUID id = UUID.randomUUID();
        when(filaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filaService.buscarFila(id))
                .isInstanceOf(FilaNotFoundException.class);
    }

    @Test
    @DisplayName("Deve ativar fila desativada")
    void shouldActivateFila() {
        fila.setActive(false);
        when(filaRepository.findById(fila.getId())).thenReturn(Optional.of(fila));
        when(filaRepository.save(any(Fila.class))).thenReturn(fila);

        FilaResponse response = filaService.ativarFila(fila.getId());

        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("Deve desativar fila ativa")
    void shouldDeactivateFila() {
        when(filaRepository.findById(fila.getId())).thenReturn(Optional.of(fila));
        when(filaRepository.save(any(Fila.class))).thenReturn(fila);

        FilaResponse response = filaService.desativarFila(fila.getId());

        assertThat(response.active()).isFalse();
    }

    private Fila createFila(UUID id, String name, String description) {
        Fila f = new Fila();
        f.setId(id);
        f.setName(name);
        f.setDescription(description);
        f.setActive(true);
        return f;
    }
}
