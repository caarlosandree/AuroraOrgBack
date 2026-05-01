package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.entity.Fila;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FilaRepository extends JpaRepository<Fila, UUID> {

    List<Fila> findAllByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
