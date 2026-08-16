package com.launchforge.shared.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.launchforge.inventory.application.InsufficientCapacityException;
import com.launchforge.shared.api.ProblemDetailsFactory;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ProblemDetailsFactory.problem(
                        HttpStatus.CONFLICT,
                        "Duplicate email",
                        exception.getMessage(),
                        request.getRequestURI(),
                        "auth/duplicate-email"
                )
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ProblemDetailsFactory.problem(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials",
                        exception.getMessage(),
                        request.getRequestURI(),
                        "auth/invalid-credentials"
                )
        );
    }

    @ExceptionHandler(ApiNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            ApiNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ProblemDetailsFactory.problem(
                        HttpStatus.NOT_FOUND,
                        exception.getTitle(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        exception.getTypeSuffix()
                )
        );
    }

    @ExceptionHandler(ApiConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ApiConflictException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ProblemDetailsFactory.problem(
                        HttpStatus.CONFLICT,
                        exception.getTitle(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        exception.getTypeSuffix()
                )
        );
    }

    @ExceptionHandler(InsufficientCapacityException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientCapacity(
            InsufficientCapacityException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetailsFactory.problem(
                HttpStatus.CONFLICT,
                exception.getTitle(),
                exception.getMessage(),
                request.getRequestURI(),
                exception.getTypeSuffix()
        );

        problem.setProperty(
                "productId",
                exception.getProductId()
        );
        problem.setProperty(
                "sku",
                exception.getSku()
        );
        problem.setProperty(
                "productName",
                exception.getProductName()
        );
        problem.setProperty(
                "availableQuantity",
                exception.getAvailableQuantity()
        );
        problem.setProperty(
                "requestedQuantity",
                exception.getRequestedQuantity()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ProblemDetailsFactory.problem(
                        HttpStatus.CONFLICT,
                        "Inventory conflict",
                        "Inventory was updated by another request. Reload and retry.",
                        request.getRequestURI(),
                        "inventory/optimistic-lock-conflict"
                )
        );
    }

    @ExceptionHandler(ApiBadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(
            ApiBadRequestException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                ProblemDetailsFactory.problem(
                        HttpStatus.BAD_REQUEST,
                        exception.getTitle(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        exception.getTypeSuffix()
                )
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ProblemDetailsFactory.problem(
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        exception.getMessage(),
                        request.getRequestURI(),
                        "orders/forbidden"
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ProblemDetailsFactory.problem(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Internal server error",
                                "An unexpected error occurred.",
                                request.getRequestURI(),
                                "internal-server-error"
                        )
                );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        String detail = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        ProblemDetail problemDetail = ProblemDetailsFactory.problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                detail,
                ((ServletWebRequest) request)
                        .getRequest()
                        .getRequestURI(),
                "validation"
        );

        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField()
                + ": "
                + fieldError.getDefaultMessage();
    }
}
