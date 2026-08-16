package com.launchforge.report.api;

import com.launchforge.report.api.dto.ActiveProductReport;
import com.launchforge.report.api.dto.DashboardReport;
import com.launchforge.report.api.dto.TopCustomerReport;
import com.launchforge.report.api.dto.TopProductReport;
import com.launchforge.report.application.ReportQueryService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {
    private final ReportQueryService reportQueryService;

    public ReportController(ReportQueryService reportQueryService) {
        this.reportQueryService = reportQueryService;
    }

    @GetMapping("/active-products")
    public List<ActiveProductReport> activeProducts() {
        return reportQueryService.activeProducts();
    }

    @GetMapping("/top-products")
    public List<TopProductReport> topProducts() {
        return reportQueryService.topProducts();
    }

    @GetMapping("/top-customers")
    public List<TopCustomerReport> topCustomers() {
        return reportQueryService.topCustomers();
    }

    @GetMapping("/dashboard")
    public DashboardReport dashboard() {
        return reportQueryService.dashboard();
    }
}
