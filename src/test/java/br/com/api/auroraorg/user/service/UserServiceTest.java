package br.com.api.auroraorg.user.service;

import br.com.api.auroraorg.shared.exception.EmailAlreadyExistsException;
import br.com.api.auroraorg.shared.exception.ResourceNotFoundException;
import br.com.api.auroraorg.user.dto.CreateUserRequest;
import br.com.api.auroraorg.user.dto.UpdateUserRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .passwordHash("encoded_password")
                .role(UserRole.SOLICITANTE)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Test
    void create_WithValidData_ShouldCreateUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
                "New User",
                "newuser@example.com",
                "password123",
                UserRole.AGENTE
        );

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .email(request.email())
                .passwordHash("encoded_password")
                .role(request.role())
                .active(true)
                .build();

        UserResponse newUserResponse = new UserResponse(
                newUser.getId(),
                newUser.getName(),
                newUser.getEmail(),
                newUser.getRole(),
                newUser.getActive(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toResponse(newUser)).thenReturn(newUserResponse);

        // When
        UserResponse response = userService.create(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("newuser@example.com");
        assertThat(response.role()).isEqualTo(UserRole.AGENTE);
    }

    @Test
    void create_WithDuplicateEmail_ShouldThrowException() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
                "New User",
                "existing@example.com",
                "password123",
                UserRole.SOLICITANTE
        );

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Já existe um usuário cadastrado com o email");
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        // Given
        User user2 = User.builder()
                .id(UUID.randomUUID())
                .name("User Two")
                .email("user2@example.com")
                .passwordHash("encoded")
                .role(UserRole.ADMIN)
                .active(true)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user, user2));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        when(userMapper.toResponse(user2)).thenReturn(new UserResponse(
                user2.getId(), user2.getName(), user2.getEmail(), user2.getRole(),
                user2.getActive(), LocalDateTime.now(), LocalDateTime.now()
        ));

        // When
        List<UserResponse> response = userService.findAll();

        // Then
        assertThat(response).hasSize(2);
    }

    @Test
    void findById_WithExistingUser_ShouldReturnUser() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // When
        UserResponse response = userService.findById(user.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(user.getId());
    }

    @Test
    void findById_WithNonExistentUser_ShouldThrowException() {
        // Given
        UUID randomId = UUID.randomUUID();
        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.findById(randomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    void activate_WithExistingUser_ShouldActivate() {
        // Given
        User inactiveUser = User.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .active(false)
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                    u.getActive(), LocalDateTime.now(), LocalDateTime.now());
        });

        // When
        UserResponse response = userService.activate(user.getId());

        // Then
        assertThat(response.active()).isTrue();
    }

    @Test
    void deactivate_WithExistingUser_ShouldDeactivate() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                    u.getActive(), LocalDateTime.now(), LocalDateTime.now());
        });

        // When
        UserResponse response = userService.deactivate(user.getId());

        // Then
        assertThat(response.active()).isFalse();
    }
}
