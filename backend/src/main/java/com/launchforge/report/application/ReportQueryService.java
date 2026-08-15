package com.launchforge.report.application;

import com.launchforge.report.api.dto.ActiveProductReport;
import com.launchforge.report.api.dto.TopCustomerReport;
import com.launchforge.report.api.dto.TopProductReport;
import com.launchforge.report.infrastructure.ReportRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportQueryService {

    private final ReportRepository reportRepository;

    public ReportQueryService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<ActiveProductReport> activeProducts() {
        List<ActiveProductReport> result = new ArrayList<>();
        reportRepository.findActiveProducts().forEach(row -> result.add(
                new ActiveProductReport(row.getId(), row.getSku(), row.getName(), row.getCategory(), row.getPrice())));
        return List.copyOf(result);
    }

    public List<TopProductReport> topProducts() {
        List<TopProductReport> result = new ArrayList<>();
        reportRepository.findTopProducts().forEach(row -> result.add(
                new TopProductReport(row.getProductId(), row.getSku(), row.getName(), row.getQuantitySold())));
        return List.copyOf(result);
    }

    public List<TopCustomerReport> topCustomers() {
        List<TopCustomerReport> result = new ArrayList<>();
        reportRepository.findTopCustomers().forEach(row -> result.add(new TopCustomerReport(
                row.getCustomerId(), row.getEmail(), row.getFirstName(), row.getLastName(), row.getOrderCount())));
        return List.copyOf(result);
    }
}
