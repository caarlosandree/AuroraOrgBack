package br.com.api.auroraorg.ticket.util;

import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Utilitário de segurança para obter informações do usuário autenticado.
 * 
 * Este componente abstrai o acesso ao SecurityContext do Spring,
 * permitindo que o código de negócio obtenha o usuário autenticado
 * de forma simples e desacoplada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Obtém o email do usuário autenticado a partir do SecurityContext.
     * 
     * @return Email do usuário autenticado ou null se não autenticado
     */
    public Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }
        
        if (principal instanceof String email) {
            return Optional.of(email);
        }
        
        return Optional.empty();
    }

    /**
     * Obtém a entidade User do usuário autenticado.
     * 
     * @return User autenticado ou empty se não encontrado
     */
    public Optional<User> getCurrentUser() {
        return getCurrentUserEmail()
                .flatMap(userRepository::findByEmail);
    }

    /**
     * Obtém o usuário autenticado ou lança exceção.
     * 
     * @return User autenticado
     * @throws IllegalStateException se não houver usuário autenticado
     */
    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> {
                    log.error("Tentativa de acesso sem usuário autenticado");
                    return new IllegalStateException("Usuário não autenticado");
                });
    }

    /**
     * Verifica se o usuário autenticado tem um dos papéis especificados.
     * 
     * @param roles Papéis a verificar
     * @return true se o usuário tiver algum dos papéis
     */
    public boolean hasAnyRole(UserRole... roles) {
        return getCurrentUser()
                .map(user -> {
                    for (UserRole role : roles) {
                        if (user.getRole() == role) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Verifica se o usuário autenticado é ADMIN.
     */
    public boolean isAdmin() {
        return hasAnyRole(UserRole.ADMIN);
    }

    /**
     * Verifica se o usuário autenticado é AGENTE (atendente).
     */
    public boolean isAgent() {
        return hasAnyRole(UserRole.AGENTE);
    }

    /**
     * Verifica se o usuário autenticado é GESTOR.
     */
    public boolean isManager() {
        return hasAnyRole(UserRole.GESTOR);
    }

    /**
     * Verifica se o usuário autenticado é SOLICITANTE.
     */
    public boolean isRequester() {
        return hasAnyRole(UserRole.SOLICITANTE);
    }

    /**
     * Verifica se o usuário autenticado pode atender chamados.
     * (ADMIN, AGENTE ou GESTOR)
     */
    public boolean canAttendTickets() {
        return hasAnyRole(UserRole.ADMIN, UserRole.AGENTE, UserRole.GESTOR);
    }

    /**
     * Obtém o ID do usuário autenticado.
     * 
     * @return UUID do usuário ou null
     */
    public Optional<UUID> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    /**
     * Verifica se o usuário autenticado é o mesmo que o ID informado.
     */
    public boolean isCurrentUser(UUID userId) {
        return getCurrentUserId()
                .map(id -> id.equals(userId))
                .orElse(false);
    }
}
