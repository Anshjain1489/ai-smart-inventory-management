package com.anshtechsolution.smart_inventory.analytics;

import com.anshtechsolution.smart_inventory.analytics.dto.DailySalesResponse;
import com.anshtechsolution.smart_inventory.analytics.dto.SalesSummaryResponse;
import com.anshtechsolution.smart_inventory.analytics.dto.TopProductResponse;
import com.anshtechsolution.smart_inventory.sales.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final SaleRepository saleRepository;

    public SalesSummaryResponse getSummary(
            LocalDate from, LocalDate to) {

        validateRange(from, to);

        SalesSummaryResponse result = saleRepository.getSummary(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());

        return result;
    }

    public List<TopProductResponse> getTopProducts(
            LocalDate from, LocalDate to) {

        validateRange(from, to);

        return saleRepository.findTopProducts(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());
    }

    public List<DailySalesResponse> getDailySales(
            LocalDate from, LocalDate to) {

        validateRange(from, to);

        return saleRepository.findDailySales(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Provide a valid date range: from must be on or before to");
        }

        if (to.isAfter(from.plusDays(365))) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "The date range cannot exceed 366 calendar days");
        }
    }
}