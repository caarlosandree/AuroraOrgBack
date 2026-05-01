package br.com.api.auroraorg.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.api.auroraorg.ticket.exception.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Email duplicado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Tentativa de login com credenciais inválidas");
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ErrorResponse> handleInactiveUserException(
            InactiveUserException ex, HttpServletRequest request) {
        log.warn("Tentativa de login com usuário inativo");
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Erro de negócio: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String message = "Erro de validação: " + errors;
        log.warn("Erro de validação: {}", errors);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Erro de validação de constraint: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Credenciais inválidas");
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Email ou senha inválidos",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado: {}", request.getRequestURI());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Acesso negado. Você não tem permissão para acessar este recurso.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ========== TICKET EXCEPTIONS ==========

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFoundException(
            TicketNotFoundException ex, HttpServletRequest request) {
        log.warn("Chamado não encontrado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ========== COMMENT EXCEPTIONS ==========

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFoundException(
            br.com.api.auroraorg.ticket.exception.CommentNotFoundException ex, HttpServletRequest request) {
        log.warn("Comentário não encontrado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.CommentPermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCommentPermissionDeniedException(
            br.com.api.auroraorg.ticket.exception.CommentPermissionDeniedException ex, HttpServletRequest request) {
        log.warn("Permissão negada para comentário: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.CommentNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotAllowedException(
            br.com.api.auroraorg.ticket.exception.CommentNotAllowedException ex, HttpServletRequest request) {
        log.warn("Operação não permitida em comentário: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.CommentRemovedException.class)
    public ResponseEntity<ErrorResponse> handleCommentRemovedException(
            br.com.api.auroraorg.ticket.exception.CommentRemovedException ex, HttpServletRequest request) {
        log.warn("Tentativa de operação em comentário removido: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ========== EVENT EXCEPTIONS ==========

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.TicketEventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketEventNotFoundException(
            br.com.api.auroraorg.ticket.exception.TicketEventNotFoundException ex, HttpServletRequest request) {
        log.warn("Evento não encontrado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransitionException(
            InvalidStatusTransitionException ex, HttpServletRequest request) {
        log.warn("Transição de status inválida: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(TicketPermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTicketPermissionDeniedException(
            TicketPermissionDeniedException ex, HttpServletRequest request) {
        log.warn("Permissão negada para chamado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(InvalidTicketOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTicketOperationException(
            InvalidTicketOperationException ex, HttpServletRequest request) {
        log.warn("Operação inválida em chamado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ========== ANEXO EXCEPTIONS ==========

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.AnexoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnexoNotFoundException(
            br.com.api.auroraorg.ticket.exception.AnexoNotFoundException ex, HttpServletRequest request) {
        log.warn("Anexo não encontrado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.ArquivoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleArquivoInvalidoException(
            br.com.api.auroraorg.ticket.exception.ArquivoInvalidoException ex, HttpServletRequest request) {
        log.warn("Arquivo inválido: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.AnexoPermissaoNegadaException.class)
    public ResponseEntity<ErrorResponse> handleAnexoPermissaoNegadaException(
            br.com.api.auroraorg.ticket.exception.AnexoPermissaoNegadaException ex, HttpServletRequest request) {
        log.warn("Permissão negada para anexo: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.LimiteAnexosExcedidoException.class)
    public ResponseEntity<ErrorResponse> handleLimiteAnexosExcedidoException(
            br.com.api.auroraorg.ticket.exception.LimiteAnexosExcedidoException ex, HttpServletRequest request) {
        log.warn("Limite de anexos excedido: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable Entity",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.AnexoStorageException.class)
    public ResponseEntity<ErrorResponse> handleAnexoStorageException(
            br.com.api.auroraorg.ticket.exception.AnexoStorageException ex, HttpServletRequest request) {
        log.error("Erro de storage de anexo: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Erro ao processar o arquivo. Tente novamente.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.ChamadoNaoPermiteAnexoException.class)
    public ResponseEntity<ErrorResponse> handleChamadoNaoPermiteAnexoException(
            br.com.api.auroraorg.ticket.exception.ChamadoNaoPermiteAnexoException ex, HttpServletRequest request) {
        log.warn("Chamado não permite anexo: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable Entity",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    // ========== FILA E CATEGORIA EXCEPTIONS ==========

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.FilaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFilaNotFoundException(
            br.com.api.auroraorg.ticket.exception.FilaNotFoundException ex, HttpServletRequest request) {
        log.warn("Fila não encontrada: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.CategoriaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoriaNotFoundException(
            br.com.api.auroraorg.ticket.exception.CategoriaNotFoundException ex, HttpServletRequest request) {
        log.warn("Categoria não encontrada: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.FilaDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleFilaDuplicadaException(
            br.com.api.auroraorg.ticket.exception.FilaDuplicadaException ex, HttpServletRequest request) {
        log.warn("Fila duplicada: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.CategoriaDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleCategoriaDuplicadaException(
            br.com.api.auroraorg.ticket.exception.CategoriaDuplicadaException ex, HttpServletRequest request) {
        log.warn("Categoria duplicada: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.AgenteNaoPermitidoException.class)
    public ResponseEntity<ErrorResponse> handleAgenteNaoPermitidoException(
            br.com.api.auroraorg.ticket.exception.AgenteNaoPermitidoException ex, HttpServletRequest request) {
        log.warn("Agente não permitido: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.AgenteJaVinculadoException.class)
    public ResponseEntity<ErrorResponse> handleAgenteJaVinculadoException(
            br.com.api.auroraorg.ticket.exception.AgenteJaVinculadoException ex, HttpServletRequest request) {
        log.warn("Agente já vinculado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.AgenteNaoVinculadoException.class)
    public ResponseEntity<ErrorResponse> handleAgenteNaoVinculadoException(
            br.com.api.auroraorg.ticket.exception.AgenteNaoVinculadoException ex, HttpServletRequest request) {
        log.warn("Agente não vinculado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.ChamadoNaoPodeSerAtribuidoException.class)
    public ResponseEntity<ErrorResponse> handleChamadoNaoPodeSerAtribuidoException(
            br.com.api.auroraorg.ticket.exception.ChamadoNaoPodeSerAtribuidoException ex, HttpServletRequest request) {
        log.warn("Chamado não pode ser atribuído: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(br.com.api.auroraorg.ticket.exception.ResponsavelNaoPertenceFilaException.class)
    public ResponseEntity<ErrorResponse> handleResponsavelNaoPertenceFilaException(
            br.com.api.auroraorg.ticket.exception.ResponsavelNaoPertenceFilaException ex, HttpServletRequest request) {
        log.warn("Responsável não pertence à fila: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ========== GLOBAL EXCEPTION ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocorreu um erro interno no servidor. Tente novamente mais tarde.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path
    ) {
    }
}
