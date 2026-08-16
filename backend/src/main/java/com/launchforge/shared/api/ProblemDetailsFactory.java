package com.launchforge.shared.api;

import java.net.URI;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetailsFactory {

    private ProblemDetailsFactory() {
    }

    public static ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            String instance,
            String typeSuffix
    ) {
        HttpStatus nonNullStatus = Objects.requireNonNull(
                status,
                "HTTP status must not be null"
        );

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        nonNullStatus,
                        detail
                );

        problemDetail.setTitle(title);

        URI type = Objects.requireNonNull(
                URI.create(
                        "https://launchforge/errors/" + typeSuffix
                ),
                "Problem type URI must not be null"
        );

        problemDetail.setType(type);

        if (instance != null) {
            URI instanceUri = Objects.requireNonNull(
                    URI.create(instance),
                    "Problem instance URI must not be null"
            );

            problemDetail.setInstance(instanceUri);
        }

        return problemDetail;
    }
}
