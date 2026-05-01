package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Categoria;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.exception.CategoriaDuplicadaException;
import br.com.api.auroraorg.ticket.exception.CategoriaNotFoundException;
import br.com.api.auroraorg.ticket.exception.FilaNotFoundException;
import br.com.api.auroraorg.ticket.repository.CategoriaRepository;
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
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private FilaRepository filaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria;
    private Fila filaPadrao;

    @BeforeEach
    void setUp() {
        filaPadrao = createFila(UUID.randomUUID(), "Suporte Técnico");
        categoria = createCategoria(UUID.randomUUID(), "Erro no sistema", "Problemas técnicos", filaPadrao);
    }

    @Test
    @DisplayName("Deve criar categoria com sucesso")
    void shouldCreateCategoria() {
        when(categoriaRepository.existsByNameIgnoreCase("Erro no sistema")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        CriarCategoriaRequest request = new CriarCategoriaRequest("Erro no sistema", "Problemas técnicos");
        CategoriaResponse response = categoriaService.criarCategoria(request);

        assertThat(response.name()).isEqualTo("Erro no sistema");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar categoria duplicada")
    void shouldThrowWhenCreatingDuplicateCategoria() {
        when(categoriaRepository.existsByNameIgnoreCase("Erro no sistema")).thenReturn(true);

        CriarCategoriaRequest request = new CriarCategoriaRequest("Erro no sistema", "Problemas técnicos");

        assertThatThrownBy(() -> categoriaService.criarCategoria(request))
                .isInstanceOf(CategoriaDuplicadaException.class);
    }

    @Test
    @DisplayName("Deve listar apenas categorias ativas")
    void shouldListActiveCategorias() {
        when(categoriaRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(categoria));

        List<CategoriaResponse> result = categoriaService.listarCategorias(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Erro no sistema");
    }

    @Test
    @DisplayName("Deve buscar categoria por ID")
    void shouldFindCategoriaById() {
        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(categoriaRepository.countTicketsByCategoriaId(categoria.getId())).thenReturn(5L);

        CategoriaDetalheResponse response = categoriaService.buscarCategoriaDetalhe(categoria.getId());

        assertThat(response.id()).isEqualTo(categoria.getId());
        assertThat(response.totalChamados()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Deve definir fila padrão da categoria")
    void shouldSetFilaPadrao() {
        Categoria semFila = createCategoria(UUID.randomUUID(), "Dúvida", "Dúvidas gerais", null);
        Fila novaFila = createFila(UUID.randomUUID(), "Comercial");

        when(categoriaRepository.findById(semFila.getId())).thenReturn(Optional.of(semFila));
        when(filaRepository.findById(novaFila.getId())).thenReturn(Optional.of(novaFila));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        DefinirFilaPadraoCategoriaRequest request = new DefinirFilaPadraoCategoriaRequest(novaFila.getId());
        CategoriaResponse response = categoriaService.definirFilaPadrao(semFila.getId(), request);

        assertThat(response.filaPadrao()).isNotNull();
        assertThat(response.filaPadrao().id()).isEqualTo(novaFila.getId());
    }

    @Test
    @DisplayName("Deve lançar exceção ao definir fila padrão inexistente")
    void shouldThrowWhenFilaPadraoNotFound() {
        UUID categoriaId = categoria.getId();
        UUID filaId = UUID.randomUUID();

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(filaRepository.findById(filaId)).thenReturn(Optional.empty());

        DefinirFilaPadraoCategoriaRequest request = new DefinirFilaPadraoCategoriaRequest(filaId);

        assertThatThrownBy(() -> categoriaService.definirFilaPadrao(categoriaId, request))
                .isInstanceOf(FilaNotFoundException.class);
    }

    private Categoria createCategoria(UUID id, String name, String description, Fila filaPadrao) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setName(name);
        c.setDescription(description);
        c.setActive(true);
        c.setFilaPadrao(filaPadrao);
        return c;
    }

    private Fila createFila(UUID id, String name) {
        Fila f = new Fila();
        f.setId(id);
        f.setName(name);
        f.setActive(true);
        return f;
    }
}
