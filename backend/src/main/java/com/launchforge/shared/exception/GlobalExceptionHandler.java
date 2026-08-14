package com.launchforge.shared.exception;

import com.launchforge.shared.api.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ProblemDetail> handleDuplicateEmail(DuplicateEmailException exception, HttpServletRequest request) {
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
    ResponseEntity<ProblemDetail> handleInvalidCredentials(
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

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
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
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        ProblemDetail problemDetail = ProblemDetailsFactory.problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                detail,
                ((ServletWebRequest) request).getRequest().getRequestURI(),
                "validation"
        );
        return ResponseEntity.badRequest().body(problemDetail);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
