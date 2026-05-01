package br.com.api.auroraorg.ticket.service.strategy;

import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.user.entity.User;

import java.util.Optional;

/**
 * Interface estratégica para distribuição automática de chamados.
 *
 * No MVP, apenas atribuição manual é implementada.
 * Futuras implementações podem incluir:
 * - Distribuição por rodízio (round-robin)
 * - Distribuição por carga de trabalho
 * - Distribuição por disponibilidade
 * - Distribuição por regras de categoria
 * - Distribuição por cliente/empresa
 */
public interface ChamadoDistribuicaoStrategy {

    /**
     * Seleciona o próximo agente disponível para atender um chamado.
     *
     * @param ticket Chamado a ser distribuído
     * @return Optional com o agente selecionado, ou empty se nenhum elegível
     */
    Optional<User> selecionarAgente(Ticket ticket);

    /**
     * Nome identificador da estratégia.
     */
    String getNome();
}
