package com.launchforge.shared.web;

public final class RequestAuditContext {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private RequestAuditContext() {
    }

    public static void set(String correlationId, String ipAddress) {
        CURRENT.set(new Context(correlationId, ipAddress));
    }

    public static Context current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Context(String correlationId, String ipAddress) {
    }
}
