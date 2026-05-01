package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findAllByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.categoria.id = :categoriaId")
    long countTicketsByCategoriaId(UUID categoriaId);
}
