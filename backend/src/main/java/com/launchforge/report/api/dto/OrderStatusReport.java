package com.launchforge.report.api.dto;

public record OrderStatusReport(long pending, long confirmed, long completed, long cancelled) {}
