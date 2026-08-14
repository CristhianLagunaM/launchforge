package com.launchforge.shared.api;

import java.net.URI;
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
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://launchforge/errors/" + typeSuffix));
        if (instance != null) {
            problemDetail.setInstance(URI.create(instance));
        }
        return problemDetail;
    }
}
