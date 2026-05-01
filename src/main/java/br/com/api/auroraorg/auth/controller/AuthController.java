package br.com.api.auroraorg.auth.controller;

import br.com.api.auroraorg.auth.dto.LoginRequest;
import br.com.api.auroraorg.auth.dto.LoginResponse;
import br.com.api.auroraorg.auth.dto.RegisterRequest;
import br.com.api.auroraorg.auth.service.AuthService;
import br.com.api.auroraorg.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para autenticação e registro de usuários")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Realizar login", description = "Autentica um usuário com email e senha, retornando um token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "403", description = "Usuário inativo"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuário", description = "Cria um novo usuário no sistema (disponível no MVP)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "409", description = "Email já cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Obter usuário autenticado", description = "Retorna os dados do usuário atualmente autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados do usuário retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.me());
    }
}
