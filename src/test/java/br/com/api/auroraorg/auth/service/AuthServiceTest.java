package br.com.api.auroraorg.auth.service;

import br.com.api.auroraorg.auth.dto.LoginRequest;
import br.com.api.auroraorg.auth.dto.LoginResponse;
import br.com.api.auroraorg.auth.dto.RegisterRequest;
import br.com.api.auroraorg.auth.security.JwtService;
import br.com.api.auroraorg.shared.exception.EmailAlreadyExistsException;
import br.com.api.auroraorg.shared.exception.InactiveUserException;
import br.com.api.auroraorg.shared.exception.InvalidCredentialsException;
import br.com.api.auroraorg.user.dto.UserResponse;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.user.mapper.UserMapper;
import br.com.api.auroraorg.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private User inactiveUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .passwordHash("encoded_password")
                .role(UserRole.SOLICITANTE)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .name("Inactive User")
                .email("inactive@example.com")
                .passwordHash("encoded_password")
                .role(UserRole.SOLICITANTE)
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userResponse = new UserResponse(
                activeUser.getId(),
                activeUser.getName(),
                activeUser.getEmail(),
                activeUser.getRole(),
                activeUser.getActive(),
                activeUser.getCreatedAt(),
                activeUser.getUpdatedAt()
        );
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(jwtService.generateToken(activeUser)).thenReturn("jwt_token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);
        when(userMapper.toResponse(activeUser)).thenReturn(userResponse);

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt_token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(7200L);
        assertThat(response.user()).isEqualTo(userResponse);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowException() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "wrong_password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email ou senha inválidos");
    }

    @Test
    void login_WithInactiveUser_ShouldThrowException() {
        // Given
        LoginRequest request = new LoginRequest("inactive@example.com", "password123");

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactiveUser));

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InactiveUserException.class)
                .hasMessage("Usuário inativo. Entre em contato com o administrador do sistema.");
    }

    @Test
    void login_WithNonExistentUser_ShouldThrowException() {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email ou senha inválidos");
    }

    @Test
    void register_WithValidData_ShouldCreateUser() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "New User",
                "newuser@example.com",
                "password123",
                UserRole.SOLICITANTE
        );

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .email(request.email())
                .passwordHash("encoded_password")
                .role(request.role())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserResponse newUserResponse = new UserResponse(
                newUser.getId(),
                newUser.getName(),
                newUser.getEmail(),
                newUser.getRole(),
                newUser.getActive(),
                newUser.getCreatedAt(),
                newUser.getUpdatedAt()
        );

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toResponse(newUser)).thenReturn(newUserResponse);

        // When
        UserResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("newuser@example.com");
        assertThat(response.role()).isEqualTo(UserRole.SOLICITANTE);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithDuplicateEmail_ShouldThrowException() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "New User",
                "existing@example.com",
                "password123",
                UserRole.SOLICITANTE
        );

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Já existe um usuário cadastrado com o email");
    }
}
