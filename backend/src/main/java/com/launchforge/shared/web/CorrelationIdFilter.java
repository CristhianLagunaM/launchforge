package com.launchforge.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String correlationId = supplied != null && VALID.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
        response.setHeader(HEADER, correlationId);
        RequestAuditContext.set(correlationId, request.getRemoteAddr());
        MDC.put("correlationId", correlationId);
        MDC.put("requestPath", request.getRequestURI());
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            MDC.put("userId", jwt.getSubject());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            RequestAuditContext.clear();
            MDC.clear();
        }
    }
}
