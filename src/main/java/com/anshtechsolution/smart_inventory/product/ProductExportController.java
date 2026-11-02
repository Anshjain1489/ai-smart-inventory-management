package com.anshtechsolution.smart_inventory.product;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/export")
@RequiredArgsConstructor
public class ProductExportController {

    private final ProductExportService productExportService;

    @GetMapping(value = "/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportProductsToCsv(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        byte[] csvData = productExportService.exportProductsToCsv(
                keyword, category, status
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"inventory-products.csv\""
                )
                .contentLength(csvData.length)
                .body(csvData);
    }

    @GetMapping(
            value = "/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<byte[]> exportProductsToExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        byte[] excelData = productExportService.exportProductsToExcel(
                keyword, category, status
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"inventory-products.xlsx\""
                )
                .contentLength(excelData.length)
                .body(excelData);
    }
}