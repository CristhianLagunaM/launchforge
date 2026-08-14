package com.launchforge.shared.exception;

public class ApiNotFoundException extends RuntimeException {

    private final String title;
    private final String typeSuffix;

    public ApiNotFoundException(String title, String detail, String typeSuffix) {
        super(detail);
        this.title = title;
        this.typeSuffix = typeSuffix;
    }

    public String getTitle() {
        return title;
    }

    public String getTypeSuffix() {
        return typeSuffix;
    }
}
