package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Categoria;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.*;
import br.com.api.auroraorg.ticket.repository.CategoriaRepository;
import br.com.api.auroraorg.ticket.repository.FilaAgenteRepository;
import br.com.api.auroraorg.ticket.repository.FilaRepository;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamadoAtribuicaoServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private FilaRepository filaRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private FilaAgenteRepository filaAgenteRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private TicketHistoryService historyService;
    @Mock private SlaService slaService;

    @InjectMocks
    private ChamadoAtribuicaoService atribuicaoService;

    private User admin;
    private User gestor;
    private User agente;
    private User solicitante;
    private Ticket ticketAberto;
    private Fila filaSuporte;
    private Categoria categoriaErro;

    @BeforeEach
    void setUp() {
        admin = createUser(UUID.randomUUID(), "admin@test.com", "Admin", UserRole.ADMIN);
        gestor = createUser(UUID.randomUUID(), "gestor@test.com", "Gestor", UserRole.GESTOR);
        agente = createUser(UUID.randomUUID(), "agente@test.com", "Agente", UserRole.AGENTE);
        solicitante = createUser(UUID.randomUUID(), "solic@test.com", "Solicitante", UserRole.SOLICITANTE);

        filaSuporte = createFila(UUID.randomUUID(), "Suporte Técnico");
        categoriaErro = createCategoria(UUID.randomUUID(), "Erro no sistema", filaSuporte);

        ticketAberto = createTicket(UUID.randomUUID(), "Chamado aberto", solicitante, null, TicketStatus.ABERTO, categoriaErro, filaSuporte);
    }

    @Test
    @DisplayName("Deve transferir chamado para outra fila com sucesso")
    void shouldTransferirFila() {
        Fila novaFila = createFila(UUID.randomUUID(), "Financeiro");

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
        when(ticketRepository.findById(ticketAberto.getId())).thenReturn(Optional.of(ticketAberto));
        when(filaRepository.findById(novaFila.getId())).thenReturn(Optional.of(novaFila));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferirFilaChamadoRequest request = new TransferirFilaChamadoRequest(novaFila.getId());
        ChamadoAtribuicaoResponse response = atribuicaoService.transferirFila(ticketAberto.getId(), request);

        assertThat(response.fila().name()).isEqualTo("Financeiro");
    }

    @Test
    @DisplayName("Deve lançar exceção ao transferir chamado fechado")
    void shouldThrowWhenTransferringClosedTicket() {
        Ticket fechado = createTicket(UUID.randomUUID(), "Fechado", solicitante, null, TicketStatus.FECHADO, null, null);

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
        when(ticketRepository.findById(fechado.getId())).thenReturn(Optional.of(fechado));

        TransferirFilaChamadoRequest request = new TransferirFilaChamadoRequest(UUID.randomUUID());

        assertThatThrownBy(() -> atribuicaoService.transferirFila(fechado.getId(), request))
                .isInstanceOf(ChamadoNaoPodeSerAtribuidoException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção quando responsável não pertence à nova fila")
    void shouldThrowWhenResponsavelNotInNewFila() {
        Fila novaFila = createFila(UUID.randomUUID(), "Financeiro");
        Ticket comResponsavel = createTicket(UUID.randomUUID(), "Com responsável", solicitante, agente, TicketStatus.EM_ATENDIMENTO, categoriaErro, filaSuporte);

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
        when(ticketRepository.findById(comResponsavel.getId())).thenReturn(Optional.of(comResponsavel));
        when(filaRepository.findById(novaFila.getId())).thenReturn(Optional.of(novaFila));
        when(filaAgenteRepository.isAgenteInFila(novaFila.getId(), agente.getId())).thenReturn(false);

        TransferirFilaChamadoRequest request = new TransferirFilaChamadoRequest(novaFila.getId());

        assertThatThrownBy(() -> atribuicaoService.transferirFila(comResponsavel.getId(), request))
                .isInstanceOf(ResponsavelNaoPertenceFilaException.class);
    }

    @Test
    @DisplayName("Deve alterar categoria do chamado com sucesso")
    void shouldAlterarCategoria() {
        Categoria novaCategoria = createCategoria(UUID.randomUUID(), "Dúvida", null);

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(gestor);
        when(ticketRepository.findById(ticketAberto.getId())).thenReturn(Optional.of(ticketAberto));
        when(categoriaRepository.findById(novaCategoria.getId())).thenReturn(Optional.of(novaCategoria));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        AlterarCategoriaChamadoRequest request = new AlterarCategoriaChamadoRequest(novaCategoria.getId());
        ChamadoAtribuicaoResponse response = atribuicaoService.alterarCategoria(ticketAberto.getId(), request);

        assertThat(response.categoria().name()).isEqualTo("Dúvida");
    }

    @Test
    @DisplayName("Deve atribuir responsável ao chamado com sucesso")
    void shouldAtribuirResponsavel() {
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
        when(securityUtils.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(ticketAberto.getId())).thenReturn(Optional.of(ticketAberto));
        when(userRepository.findById(agente.getId())).thenReturn(Optional.of(agente));
        when(filaAgenteRepository.isAgenteInFila(filaSuporte.getId(), agente.getId())).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        AtribuirResponsavelRequest request = new AtribuirResponsavelRequest(agente.getId());
        ChamadoAtribuicaoResponse response = atribuicaoService.atribuirResponsavel(ticketAberto.getId(), request);

        assertThat(response.responsavel().email()).isEqualTo("agente@test.com");
    }

    @Test
    @DisplayName("Deve permitir agente assumir chamado da fila")
    void shouldAssumirChamado() {
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
        when(ticketRepository.findById(ticketAberto.getId())).thenReturn(Optional.of(ticketAberto));
        when(filaAgenteRepository.isAgenteInFila(filaSuporte.getId(), agente.getId())).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ChamadoAtribuicaoResponse response = atribuicaoService.assumirChamado(ticketAberto.getId());

        assertThat(response.responsavel().email()).isEqualTo("agente@test.com");
        assertThat(response.status()).isEqualTo(TicketStatus.EM_ATENDIMENTO);
    }

    @Test
    @DisplayName("Deve impedir agente de assumir chamado de fila que não participa")
    void shouldPreventAgenteFromAssumingOutsideFila() {
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
        when(ticketRepository.findById(ticketAberto.getId())).thenReturn(Optional.of(ticketAberto));
        when(filaAgenteRepository.isAgenteInFila(filaSuporte.getId(), agente.getId())).thenReturn(false);

        assertThatThrownBy(() -> atribuicaoService.assumirChamado(ticketAberto.getId()))
                .isInstanceOf(TicketPermissionDeniedException.class)
                .hasMessageContaining("não pertence à fila");
    }

    @Test
    @DisplayName("Deve remover responsável do chamado com sucesso")
    void shouldRemoverResponsavel() {
        Ticket comAgente = createTicket(UUID.randomUUID(), "Com agente", solicitante, agente, TicketStatus.EM_ATENDIMENTO, categoriaErro, filaSuporte);

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
        when(securityUtils.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(comAgente.getId())).thenReturn(Optional.of(comAgente));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ChamadoAtribuicaoResponse response = atribuicaoService.removerResponsavel(comAgente.getId());

        assertThat(response.responsavel()).isNull();
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover responsável de chamado sem responsável")
    void shouldThrowWhenRemovingNonExistentResponsavel() {
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
        when(securityUtils.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(ticketAberto.getId())).thenReturn(Optional.of(ticketAberto));

        assertThatThrownBy(() -> atribuicaoService.removerResponsavel(ticketAberto.getId()))
                .isInstanceOf(InvalidTicketOperationException.class)
                .hasMessageContaining("não possui responsável");
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

    private Categoria createCategoria(UUID id, String name, Fila filaPadrao) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setName(name);
        c.setActive(true);
        c.setFilaPadrao(filaPadrao);
        return c;
    }

    private Ticket createTicket(UUID id, String title, User requester, User assignee, TicketStatus status, Categoria categoria, Fila fila) {
        return Ticket.builder()
                .id(id)
                .title(title)
                .description("Descrição")
                .status(status)
                .priority(TicketPriority.MEDIA)
                .requester(requester)
                .assignee(assignee)
                .categoria(categoria)
                .fila(fila)
                .slaDueAt(LocalDateTime.now().plusHours(48))
                .build();
    }
}
