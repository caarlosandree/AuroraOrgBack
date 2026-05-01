package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.config.AttachmentProperties;
import br.com.api.auroraorg.ticket.dto.UploadAnexoResponse;
import br.com.api.auroraorg.ticket.entity.ChamadoAnexo;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.entity.TicketEvent;
import br.com.api.auroraorg.ticket.enums.TicketEventType;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.*;
import br.com.api.auroraorg.ticket.repository.ChamadoAnexoRepository;
import br.com.api.auroraorg.ticket.repository.TicketEventRepository;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChamadoAnexoService")
class ChamadoAnexoServiceTest {

    @Mock private ChamadoAnexoRepository anexoRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketEventRepository eventRepository;
    @Mock private ArquivoStorageService storageService;
    @Mock private ArquivoValidacaoService validacaoService;
    @Mock private SecurityUtils securityUtils;
    @Mock private AttachmentProperties properties;

    @InjectMocks
    private ChamadoAnexoService service;

    private UUID ticketId;
    private Ticket ticket;
    private User solicitante;
    private User agente;
    private User admin;
    private User gestor;
    private AttachmentProperties.Attachments attachmentsCfg;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();

        solicitante = User.builder()
                .id(UUID.randomUUID()).name("João").email("joao@test.com")
                .role(UserRole.SOLICITANTE).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        agente = User.builder()
                .id(UUID.randomUUID()).name("Maria").email("maria@test.com")
                .role(UserRole.AGENTE).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        admin = User.builder()
                .id(UUID.randomUUID()).name("Admin").email("admin@test.com")
                .role(UserRole.ADMIN).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        gestor = User.builder()
                .id(UUID.randomUUID()).name("Gestor").email("gestor@test.com")
                .role(UserRole.GESTOR).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        ticket = Ticket.builder()
                .id(ticketId)
                .title("Chamado teste")
                .description("Descrição")
                .status(TicketStatus.ABERTO)
                .requester(solicitante)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .slaDueAt(LocalDateTime.now().plusHours(8))
                .build();

        attachmentsCfg = new AttachmentProperties.Attachments();
    }

    /** Configura o mock de properties apenas para testes que alcançam validarLimiteAnexos */
    private void stubProperties() {
        when(properties.getAttachments()).thenReturn(attachmentsCfg);
    }

    // ========== UPLOAD ==========

    @Nested
    @DisplayName("Upload de anexo")
    class Upload {

        @Test
        @DisplayName("Deve permitir solicitante anexar no próprio chamado")
        void devePermitirSolicitanteAnexarNoProprioChamado() {
            stubProperties();
            MultipartFile arquivo = pngValido("foto.png");
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(solicitante);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.countActiveByTicketId(ticketId)).thenReturn(0L);
            when(storageService.salvar(any(), any(), any())).thenReturn("chamados/" + ticketId + "/anexos/xyz.png");
            when(anexoRepository.save(any())).thenAnswer(inv -> {
                ChamadoAnexo a = inv.getArgument(0);
                a.prePersist();
                return a;
            });
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UploadAnexoResponse response = service.upload(ticketId, arquivo);

            assertThat(response.originalFileName()).isEqualTo("foto.png");
            assertThat(response.contentType()).isEqualTo("image/png");
            verify(storageService).salvar(any(), eq(ticketId), any());
        }

        @Test
        @DisplayName("Deve permitir agente anexar em chamado")
        void devePermitirAgenteAnexar() {
            stubProperties();
            MultipartFile arquivo = pdfValido("relatorio.pdf");
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.countActiveByTicketId(ticketId)).thenReturn(0L);
            when(storageService.salvar(any(), any(), any())).thenReturn("chamados/" + ticketId + "/anexos/abc.pdf");
            when(anexoRepository.save(any())).thenAnswer(inv -> {
                ChamadoAnexo a = inv.getArgument(0);
                a.prePersist();
                return a;
            });
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UploadAnexoResponse response = service.upload(ticketId, arquivo);

            assertThat(response.originalFileName()).isEqualTo("relatorio.pdf");
        }

        @Test
        @DisplayName("Deve bloquear solicitante tentando anexar em chamado de outro usuário")
        void deveBloquearSolicitanteEmChamadoAlheio() {
            Ticket chamadoAlheio = Ticket.builder()
                    .id(ticketId).title("Alheio").description("x").status(TicketStatus.ABERTO)
                    .requester(agente) // solicitante é o agente, não o solicitante
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .slaDueAt(LocalDateTime.now().plusHours(4))
                    .build();

            when(securityUtils.getCurrentUserOrThrow()).thenReturn(solicitante);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(chamadoAlheio));

            assertThatThrownBy(() -> service.upload(ticketId, pngValido("x.png")))
                    .isInstanceOf(AnexoPermissaoNegadaException.class)
                    .hasMessageContaining("próprio chamado");
        }

        @Test
        @DisplayName("Deve bloquear GESTOR de fazer upload")
        void deveBloquearGestor() {
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(gestor);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> service.upload(ticketId, pngValido("x.png")))
                    .isInstanceOf(AnexoPermissaoNegadaException.class);
        }

        @Test
        @DisplayName("Deve bloquear upload quando chamado está FECHADO (não-ADMIN)")
        void deveBloquearUploadEmChamadoFechado() {
            ticket.setStatus(TicketStatus.FECHADO);
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> service.upload(ticketId, pngValido("x.png")))
                    .isInstanceOf(ChamadoNaoPermiteAnexoException.class);
        }

        @Test
        @DisplayName("Deve permitir ADMIN fazer upload em chamado FECHADO")
        void devePermitirAdminUploadEmFechado() {
            stubProperties();
            ticket.setStatus(TicketStatus.FECHADO);
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.countActiveByTicketId(ticketId)).thenReturn(0L);
            when(storageService.salvar(any(), any(), any())).thenReturn("chamados/" + ticketId + "/anexos/z.png");
            when(anexoRepository.save(any())).thenAnswer(inv -> {
                ChamadoAnexo a = inv.getArgument(0);
                a.prePersist();
                return a;
            });
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UploadAnexoResponse response = service.upload(ticketId, pngValido("z.png"));
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Deve bloquear upload quando limite de anexos é atingido")
        void deveBloquearUploadAcimaDeLimite() {
            stubProperties();
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.countActiveByTicketId(ticketId)).thenReturn(20L); // limite

            assertThatThrownBy(() -> service.upload(ticketId, pngValido("x.png")))
                    .isInstanceOf(LimiteAnexosExcedidoException.class);
        }

        @Test
        @DisplayName("Deve registrar evento ANEXO_ADICIONADO após upload")
        void deveRegistrarEventoAnexoAdicionado() {
            stubProperties();
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.countActiveByTicketId(ticketId)).thenReturn(0L);
            when(storageService.salvar(any(), any(), any())).thenReturn("path/xyz.png");
            when(anexoRepository.save(any())).thenAnswer(inv -> {
                ChamadoAnexo a = inv.getArgument(0);
                a.prePersist();
                return a;
            });
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.upload(ticketId, pngValido("teste.png"));

            ArgumentCaptor<TicketEvent> captor = ArgumentCaptor.forClass(TicketEvent.class);
            verify(eventRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(TicketEventType.ANEXO_ADICIONADO);
        }
    }

    // ========== REMOÇÃO ==========

    @Nested
    @DisplayName("Remoção lógica de anexo")
    class Remocao {

        private ChamadoAnexo anexo;
        private UUID anexoId;

        @BeforeEach
        void setUpAnexo() {
            anexoId = UUID.randomUUID();
            anexo = ChamadoAnexo.builder()
                    .id(anexoId)
                    .ticket(ticket)
                    .fileName("abc.png")
                    .originalFileName("foto.png")
                    .contentType("image/png")
                    .fileSize(1024L)
                    .storagePath("chamados/" + ticketId + "/anexos/abc.png")
                    .uploadedBy(agente)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Deve remover logicamente um anexo e registrar evento")
        void deveRemoverLogicamenteERegistrarEvento() {
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.findActiveByIdAndTicketId(anexoId, ticketId)).thenReturn(Optional.of(anexo));
            when(anexoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.remover(ticketId, anexoId);

            assertThat(anexo.isRemoved()).isTrue();
            assertThat(anexo.getRemovedAt()).isNotNull();
            assertThat(anexo.getRemovedBy()).isEqualTo(agente);

            ArgumentCaptor<TicketEvent> captor = ArgumentCaptor.forClass(TicketEvent.class);
            verify(eventRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(TicketEventType.ANEXO_REMOVIDO);
        }

        @Test
        @DisplayName("Deve bloquear download de anexo removido")
        void deveBloquearDownloadDeAnexoRemovido() {
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.findActiveByIdAndTicketId(anexoId, ticketId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.download(ticketId, anexoId))
                    .isInstanceOf(AnexoNotFoundException.class);
        }

        @Test
        @DisplayName("Deve bloquear GESTOR de remover anexo")
        void deveBloquearGestorDeRemover() {
            when(securityUtils.getCurrentUserOrThrow()).thenReturn(gestor);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.findActiveByIdAndTicketId(anexoId, ticketId)).thenReturn(Optional.of(anexo));

            assertThatThrownBy(() -> service.remover(ticketId, anexoId))
                    .isInstanceOf(AnexoPermissaoNegadaException.class);
        }

        @Test
        @DisplayName("Deve bloquear AGENTE de remover anexo de outro usuário")
        void deveBloquearAgenteDeRemoverAnexoAlheio() {
            ChamadoAnexo anexoDoSolicitante = ChamadoAnexo.builder()
                    .id(anexoId).ticket(ticket).fileName("x.png").originalFileName("x.png")
                    .contentType("image/png").fileSize(100L)
                    .storagePath("path/x.png").uploadedBy(solicitante)
                    .createdAt(LocalDateTime.now()).build();

            when(securityUtils.getCurrentUserOrThrow()).thenReturn(agente);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(anexoRepository.findActiveByIdAndTicketId(anexoId, ticketId)).thenReturn(Optional.of(anexoDoSolicitante));

            assertThatThrownBy(() -> service.remover(ticketId, anexoId))
                    .isInstanceOf(AnexoPermissaoNegadaException.class)
                    .hasMessageContaining("próprios anexos");
        }
    }

    // ========== VALIDAÇÃO DE ARQUIVO ==========

    @Nested
    @DisplayName("Validação de arquivo via ArquivoValidacaoService (unitário)")
    class ValidacaoArquivo {

        private ArquivoValidacaoService validacao;

        @BeforeEach
        void setUpValidacao() {
            AttachmentProperties props = new AttachmentProperties();
            validacao = new ArquivoValidacaoService(props);
        }

        @Test
        @DisplayName("Deve bloquear arquivo vazio")
        void deveBloquearArquivoVazio() {
            MockMultipartFile vazio = new MockMultipartFile("arquivo", "test.png", "image/png", new byte[0]);
            assertThatThrownBy(() -> validacao.validar(vazio))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("vazio");
        }

        @Test
        @DisplayName("Deve bloquear content-type não permitido")
        void deveBloquearContentTypeNaoPermitido() {
            MockMultipartFile exe = new MockMultipartFile("arquivo", "virus.exe",
                    "application/octet-stream", new byte[]{1, 2, 3});
            assertThatThrownBy(() -> validacao.validar(exe))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("não permitido");
        }

        @Test
        @DisplayName("Deve bloquear extensão perigosa .exe")
        void deveBloquearExtensaoPerigosa() {
            MockMultipartFile exe = new MockMultipartFile("arquivo", "virus.exe",
                    "image/png", new byte[]{1, 2, 3}); // spoofing de MIME
            assertThatThrownBy(() -> validacao.validar(exe))
                    .isInstanceOf(ArquivoInvalidoException.class);
        }

        @Test
        @DisplayName("Deve bloquear extensão .sh")
        void deveBloquearExtensaoSh() {
            MockMultipartFile sh = new MockMultipartFile("arquivo", "script.sh",
                    "text/plain", "echo hi".getBytes());
            assertThatThrownBy(() -> validacao.validar(sh))
                    .isInstanceOf(ArquivoInvalidoException.class);
        }

        @Test
        @DisplayName("Deve bloquear nome com path traversal")
        void deveBloquearPathTraversal() {
            MockMultipartFile malicioso = new MockMultipartFile("arquivo", "../../etc/passwd",
                    "image/png", new byte[]{1, 2, 3});
            assertThatThrownBy(() -> validacao.validar(malicioso))
                    .isInstanceOf(ArquivoInvalidoException.class);
        }

        @Test
        @DisplayName("Deve bloquear arquivo acima do limite de tamanho")
        void deveBloquearArquivoGrande() {
            byte[] dadosGrandes = new byte[11 * 1024 * 1024]; // 11 MB
            MockMultipartFile grande = new MockMultipartFile("arquivo", "grande.png",
                    "image/png", dadosGrandes);
            assertThatThrownBy(() -> validacao.validar(grande))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("tamanho máximo");
        }

        @Test
        @DisplayName("Deve aceitar imagem PNG válida")
        void deveAceitarPngValido() {
            MockMultipartFile png = new MockMultipartFile("arquivo", "foto.png",
                    "image/png", new byte[]{1, 2, 3, 4, 5});
            validacao.validar(png); // não deve lançar exception
        }

        @Test
        @DisplayName("Deve aceitar PDF válido")
        void deveAceitarPdfValido() {
            MockMultipartFile pdf = new MockMultipartFile("arquivo", "doc.pdf",
                    "application/pdf", new byte[]{1, 2, 3});
            validacao.validar(pdf); // não deve lançar exception
        }
    }

    // ========== SEGURANÇA: storage_path não exposto ==========

    @Nested
    @DisplayName("Segurança: storagePath não exposto")
    class SegurancaStoragePath {

        @Test
        @DisplayName("Resposta de upload não deve conter storagePath")
        void respostaUploadNaoDeveConterStoragePath() throws Exception {
            var campos = UploadAnexoResponse.class.getRecordComponents();
            boolean contemStoragePath = java.util.Arrays.stream(campos)
                    .anyMatch(c -> c.getName().toLowerCase().contains("storage")
                            || c.getName().toLowerCase().contains("path"));
            assertThat(contemStoragePath)
                    .as("UploadAnexoResponse não deve expor storagePath")
                    .isFalse();
        }

        @Test
        @DisplayName("AnexoResponse não deve conter storagePath")
        void anexoResponseNaoDeveConterStoragePath() {
            var campos = br.com.api.auroraorg.ticket.dto.AnexoResponse.class.getRecordComponents();
            boolean contemStoragePath = java.util.Arrays.stream(campos)
                    .anyMatch(c -> c.getName().toLowerCase().contains("storage")
                            || c.getName().toLowerCase().contains("path"));
            assertThat(contemStoragePath)
                    .as("AnexoResponse não deve expor storagePath")
                    .isFalse();
        }
    }

    // ========== HELPERS ==========

    private MockMultipartFile pngValido(String nome) {
        return new MockMultipartFile("arquivo", nome, "image/png", new byte[]{1, 2, 3, 4, 5});
    }

    private MockMultipartFile pdfValido(String nome) {
        return new MockMultipartFile("arquivo", nome, "application/pdf", new byte[]{1, 2, 3});
    }
}
