package br.com.api.auroraorg.shared.config;

import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Value("${ADMIN_NAME}")
    private String adminName;

    @Value("${ADMIN_ID}")
    private String adminId;

    @Bean
    public CommandLineRunner initAdminUser() {
        return args -> {
            if (!userRepository.existsByEmail(adminEmail)) {
                log.info("Criando usuário administrador inicial...");

                User admin = User.builder()
                        .id(UUID.fromString(adminId))
                        .name(adminName)
                        .email(adminEmail)
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .role(UserRole.ADMIN)
                        .active(true)
                        .build();

                userRepository.save(admin);
                log.info("Usuário administrador criado com sucesso: {}", adminEmail);
            } else {
                log.debug("Usuário administrador já existe: {}", adminEmail);
            }
        };
    }
}
