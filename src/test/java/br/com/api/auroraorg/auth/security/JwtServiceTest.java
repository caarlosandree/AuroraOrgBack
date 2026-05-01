package br.com.api.auroraorg.auth.security;

import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-key-for-testing-only", 7200);
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        // Given
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .passwordHash("encoded")
                .role(UserRole.SOLICITANTE)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("SOLICITANTE");
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void isTokenValid_WithInvalidToken_ShouldReturnFalse() {
        assertThat(jwtService.isTokenValid("invalid_token")).isFalse();
    }
}
