package br.com.api.auroraorg.user.service;

import br.com.api.auroraorg.shared.exception.EmailAlreadyExistsException;
import br.com.api.auroraorg.shared.exception.ResourceNotFoundException;
import br.com.api.auroraorg.user.dto.CreateUserRequest;
import br.com.api.auroraorg.user.dto.UpdateUserRequest;
import br.com.api.auroraorg.user.dto.UserResponse;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.mapper.UserMapper;
import br.com.api.auroraorg.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        log.info("Criando novo usuário: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Tentativa de criar usuário com email duplicado: {}", request.email());
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Usuário criado com sucesso: {}", savedUser.getId());

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        log.debug("Buscando todos os usuários");
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        log.debug("Buscando usuário por ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        log.debug("Buscando usuário por email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "email", email));
    }

    @Transactional(readOnly = true)
    public UserResponse findResponseByEmail(String email) {
        return userMapper.toResponse(findByEmail(email));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        log.info("Atualizando usuário: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id));

        user.setName(request.name());
        user.setRole(request.role());

        User updatedUser = userRepository.save(user);
        log.info("Usuário atualizado com sucesso: {}", updatedUser.getId());

        return userMapper.toResponse(updatedUser);
    }

    @Transactional
    public UserResponse activate(UUID id) {
        log.info("Ativando usuário: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id));

        user.setActive(true);
        User activatedUser = userRepository.save(user);

        log.info("Usuário ativado com sucesso: {}", activatedUser.getId());
        return userMapper.toResponse(activatedUser);
    }

    @Transactional
    public UserResponse deactivate(UUID id) {
        log.info("Desativando usuário: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id));

        user.setActive(false);
        User deactivatedUser = userRepository.save(user);

        log.info("Usuário desativado com sucesso: {}", deactivatedUser.getId());
        return userMapper.toResponse(deactivatedUser);
    }
}
