package com.anshtechsolution.smart_inventory.product;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductExportService {

    private final ProductRepository productRepository;

    public byte[] exportProductsToCsv() {
        StringBuilder csv = new StringBuilder();

        // UTF-8 BOM helps Excel recognize UTF-8 CSV files.
        csv.append('\uFEFF');

        csv.append(
                "ID,SKU,Name,Description,Category,Supplier ID,"
                        + "Quantity,Reorder Level,Unit Price,Created At,Updated At\n"
        );

        productRepository.findAll().forEach(product -> {
            csv.append(product.getId()).append(',')
                    .append(escapeCsv(product.getSku())).append(',')
                    .append(escapeCsv(product.getName())).append(',')
                    .append(escapeCsv(product.getDescription())).append(',')
                    .append(escapeCsv(product.getCategory())).append(',')
                    .append(product.getSupplierId() == null
                            ? ""
                            : product.getSupplierId()).append(',')
                    .append(product.getQuantity()).append(',')
                    .append(product.getReorderLevel()).append(',')
                    .append(product.getUnitPrice()).append(',')
                    .append(escapeCsv(
                            product.getCreatedAt() == null
                                    ? ""
                                    : product.getCreatedAt().toString()
                    )).append(',')
                    .append(escapeCsv(
                            product.getUpdatedAt() == null
                                    ? ""
                                    : product.getUpdatedAt().toString()
                    ))
                    .append("\r\n");
        });

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        // Prevent spreadsheet formula execution for untrusted text.
        String safeValue = value;
        if (safeValue.matches("^[\\s]*[=+@].*")
                || safeValue.matches("^[\\s]*-.*")) {
            safeValue = "'" + safeValue;
        }

        // Escape double quotes and wrap fields containing CSV special characters.
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    public byte[] exportProductsToExcel() {

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Inventory");

            // Create header style.
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            // Create column headers.
            String[] headers = {
                    "ID", "SKU", "Name", "Description", "Category",
                    "Supplier ID", "Quantity", "Reorder Level",
                    "Unit Price", "Created At", "Updated At"
            };

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate product rows.
            var products = productRepository.findAll();

            int rowIndex = 1;

            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);

                setNumericCell(row, 0, product.getId());
                setTextCell(row, 1, product.getSku());
                setTextCell(row, 2, product.getName());
                setTextCell(row, 3, product.getDescription());
                setTextCell(row, 4, product.getCategory());

                if (product.getSupplierId() != null) {
                    setNumericCell(row, 5, product.getSupplierId());
                }

                setNumericCell(row, 6, product.getQuantity());
                setNumericCell(row, 7, product.getReorderLevel());

                if (product.getUnitPrice() != null) {
                    setNumericCell(row, 8, product.getUnitPrice());
                }

                setTextCell(
                        row, 9,
                        product.getCreatedAt() == null
                                ? null : product.getCreatedAt().toString()
                );

                setTextCell(
                        row, 10,
                        product.getUpdatedAt() == null
                                ? null : product.getUpdatedAt().toString()
                );
            }

            // Adjust widths for readability.
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(
                        i,
                        Math.min(sheet.getColumnWidth(i) + 512, 15000)
                );
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to generate Excel inventory report",
                    exception
            );
        }
    }

    private void setTextCell(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
    }

    private void setNumericCell(Row row, int column, Number value) {
        Cell cell = row.createCell(column);

        if (value != null) {
            cell.setCellValue(new BigDecimal(value.toString()).doubleValue());
        }
    }

    public List<Product> getProductsForExport(
            String keyword,
            String category,
            String status
    ) {
        keyword = normalizeFilter(keyword);
        category = normalizeFilter(category);
        status = normalizeFilter(status);

        if (status != null) {
            status = status.toUpperCase(Locale.ROOT);

            if (!List.of("IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK")
                    .contains(status)) {
                throw new IllegalArgumentException(
                        "Invalid stock status. Allowed values: "
                                + "IN_STOCK, LOW_STOCK, OUT_OF_STOCK"
                );
            }
        }

        return productRepository.findProductsForExport(
                keyword,
                category,
                status
        );
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    public byte[] exportProductsToCsv(
            String keyword,
            String category,
            String status
    ) {
        List<Product> products =
                getProductsForExport(keyword, category, status);

        StringBuilder csv = new StringBuilder();

        // UTF-8 BOM for Excel compatibility
        csv.append('\uFEFF');

        csv.append(
                "ID,SKU,Name,Description,Category,Supplier ID,"
                        + "Quantity,Reorder Level,Unit Price,Created At,Updated At\n"
        );

        for (Product product : products) {
            csv.append(product.getId()).append(',')
                    .append(escapeCsv(product.getSku())).append(',')
                    .append(escapeCsv(product.getName())).append(',')
                    .append(escapeCsv(product.getDescription())).append(',')
                    .append(escapeCsv(product.getCategory())).append(',')
                    .append(product.getSupplierId() == null
                            ? "" : product.getSupplierId()).append(',')
                    .append(product.getQuantity()).append(',')
                    .append(product.getReorderLevel()).append(',')
                    .append(product.getUnitPrice() == null
                            ? "" : product.getUnitPrice()).append(',')
                    .append(escapeCsv(product.getCreatedAt() == null
                            ? null : product.getCreatedAt().toString())).append(',')
                    .append(escapeCsv(product.getUpdatedAt() == null
                            ? null : product.getUpdatedAt().toString()))
                    .append('\n');
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportProductsToExcel(
            String keyword,
            String category,
            String status
    ) {
        List<Product> products =
                getProductsForExport(keyword, category, status);

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Inventory");

            String[] headers = {
                    "ID", "SKU", "Name", "Description", "Category",
                    "Supplier ID", "Quantity", "Reorder Level",
                    "Unit Price", "Created At", "Updated At"
            };

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;

            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);

                setNumericCell(row, 0, product.getId());
                setTextCell(row, 1, product.getSku());
                setTextCell(row, 2, product.getName());
                setTextCell(row, 3, product.getDescription());
                setTextCell(row, 4, product.getCategory());

                if (product.getSupplierId() != null) {
                    setNumericCell(row, 5, product.getSupplierId());
                }

                setNumericCell(row, 6, product.getQuantity());
                setNumericCell(row, 7, product.getReorderLevel());

                if (product.getUnitPrice() != null) {
                    setNumericCell(row, 8, product.getUnitPrice());
                }

                setTextCell(row, 9, product.getCreatedAt() == null
                        ? null : product.getCreatedAt().toString());

                setTextCell(row, 10, product.getUpdatedAt() == null
                        ? null : product.getUpdatedAt().toString());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(
                        i,
                        Math.min(sheet.getColumnWidth(i) + 512, 15000)
                );
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to generate filtered Excel inventory report",
                    exception
            );
        }
    }
}