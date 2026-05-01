package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.FilaAgenteResponse;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.entity.FilaAgente;
import br.com.api.auroraorg.ticket.entity.FilaAgenteId;
import br.com.api.auroraorg.ticket.exception.AgenteJaVinculadoException;
import br.com.api.auroraorg.ticket.exception.AgenteNaoPermitidoException;
import br.com.api.auroraorg.ticket.exception.AgenteNaoVinculadoException;
import br.com.api.auroraorg.ticket.exception.FilaNotFoundException;
import br.com.api.auroraorg.ticket.repository.FilaAgenteRepository;
import br.com.api.auroraorg.ticket.repository.FilaRepository;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.user.repository.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilaAgenteServiceTest {

    @Mock
    private FilaAgenteRepository filaAgenteRepository;

    @Mock
    private FilaRepository filaRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FilaAgenteService filaAgenteService;

    private Fila fila;
    private User agente;
    private User adminUser;
    private User solicitante;

    @BeforeEach
    void setUp() {
        fila = createFila(UUID.randomUUID(), "Suporte Técnico");
        agente = createUser(UUID.randomUUID(), "agente@test.com", "Agente", UserRole.AGENTE);
        adminUser = createUser(UUID.randomUUID(), "admin@test.com", "Admin", UserRole.ADMIN);
        solicitante = createUser(UUID.randomUUID(), "solic@test.com", "Solicitante", UserRole.SOLICITANTE);
    }

    @Test
    @DisplayName("Deve adicionar agente à fila com sucesso")
    void shouldAddAgenteToFila() {
        when(filaRepository.findById(fila.getId())).thenReturn(Optional.of(fila));
        when(userRepository.findById(agente.getId())).thenReturn(Optional.of(agente));
        when(filaAgenteRepository.existsByFilaIdAndAgenteId(fila.getId(), agente.getId())).thenReturn(false);
        when(filaAgenteRepository.save(any(FilaAgente.class))).thenAnswer(inv -> inv.getArgument(0));

        filaAgenteService.adicionarAgente(fila.getId(), agente.getId());

        verify(filaAgenteRepository).save(any(FilaAgente.class));
    }

    @Test
    @DisplayName("Deve permitir adicionar ADMIN à fila")
    void shouldAllowAdminInFila() {
        when(filaRepository.findById(fila.getId())).thenReturn(Optional.of(fila));
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(filaAgenteRepository.existsByFilaIdAndAgenteId(fila.getId(), adminUser.getId())).thenReturn(false);
        when(filaAgenteRepository.save(any(FilaAgente.class))).thenAnswer(inv -> inv.getArgument(0));

        filaAgenteService.adicionarAgente(fila.getId(), adminUser.getId());

        verify(filaAgenteRepository).save(any(FilaAgente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar SOLICITANTE à fila")
    void shouldThrowWhenAddingSolicitante() {
        when(filaRepository.findById(fila.getId())).thenReturn(Optional.of(fila));
        when(userRepository.findById(solicitante.getId())).thenReturn(Optional.of(solicitante));

        assertThatThrownBy(() -> filaAgenteService.adicionarAgente(fila.getId(), solicitante.getId()))
                .isInstanceOf(AgenteNaoPermitidoException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar agente já vinculado")
    void shouldThrowWhenAgenteAlreadyVinculado() {
        when(filaRepository.findById(fila.getId())).thenReturn(Optional.of(fila));
        when(userRepository.findById(agente.getId())).thenReturn(Optional.of(agente));
        when(filaAgenteRepository.existsByFilaIdAndAgenteId(fila.getId(), agente.getId())).thenReturn(true);

        assertThatThrownBy(() -> filaAgenteService.adicionarAgente(fila.getId(), agente.getId()))
                .isInstanceOf(AgenteJaVinculadoException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar agente em fila inexistente")
    void shouldThrowWhenFilaNotFoundOnAdd() {
        UUID filaId = UUID.randomUUID();
        when(filaRepository.findById(filaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filaAgenteService.adicionarAgente(filaId, agente.getId()))
                .isInstanceOf(FilaNotFoundException.class);
    }

    @Test
    @DisplayName("Deve remover agente da fila com sucesso")
    void shouldRemoveAgenteFromFila() {
        when(filaRepository.existsById(fila.getId())).thenReturn(true);
        when(filaAgenteRepository.existsByFilaIdAndAgenteId(fila.getId(), agente.getId())).thenReturn(true);
        doNothing().when(filaAgenteRepository).deleteById(any(FilaAgenteId.class));

        filaAgenteService.removerAgente(fila.getId(), agente.getId());

        verify(filaAgenteRepository).deleteById(any(FilaAgenteId.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover agente não vinculado")
    void shouldThrowWhenAgenteNotVinculado() {
        when(filaRepository.existsById(fila.getId())).thenReturn(true);
        when(filaAgenteRepository.existsByFilaIdAndAgenteId(fila.getId(), agente.getId())).thenReturn(false);

        assertThatThrownBy(() -> filaAgenteService.removerAgente(fila.getId(), agente.getId()))
                .isInstanceOf(AgenteNaoVinculadoException.class);
    }

    @Test
    @DisplayName("Deve listar agentes da fila")
    void shouldListAgentesDaFila() {
        when(filaRepository.existsById(fila.getId())).thenReturn(true);

        FilaAgente fa = FilaAgente.builder()
                .id(new FilaAgenteId(fila.getId(), agente.getId()))
                .fila(fila)
                .agente(agente)
                .build();

        when(filaAgenteRepository.findAllByFilaIdWithAgente(fila.getId())).thenReturn(List.of(fa));

        List<FilaAgenteResponse> result = filaAgenteService.listarAgentesDaFila(fila.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).agente().email()).isEqualTo("agente@test.com");
    }

    private User createUser(UUID id, String email, String name, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Fila createFila(UUID id, String name) {
        Fila f = new Fila();
        f.setId(id);
        f.setName(name);
        f.setActive(true);
        return f;
    }
}
