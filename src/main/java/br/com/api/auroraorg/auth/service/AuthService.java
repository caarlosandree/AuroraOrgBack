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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Tentativa de login: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Tentativa de login com usuário inexistente: {}", request.email());
                    return new InvalidCredentialsException();
                });

        if (!user.getActive()) {
            log.warn("Tentativa de login com usuário inativo: {}", request.email());
            throw new InactiveUserException();
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            log.warn("Tentativa de login com senha inválida: {}", request.email());
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);
        long expiresIn = jwtService.getExpirationSeconds();

        log.info("Login bem-sucedido: {}", request.email());

        return new LoginResponse(
                token,
                "Bearer",
                expiresIn,
                userMapper.toResponse(user)
        );
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Registro de novo usuário: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Tentativa de registro com email duplicado: {}", request.email());
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
        log.info("Usuário registrado com sucesso: {}", savedUser.getId());

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        log.debug("Buscando usuário autenticado: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        return userMapper.toResponse(user);
    }
}
