package com.anshtechsolution.smart_inventory.sales;

import com.anshtechsolution.smart_inventory.exception.ProductNotFoundException;
import com.anshtechsolution.smart_inventory.inventory.StockMovement;
import com.anshtechsolution.smart_inventory.inventory.StockMovementRepository;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import com.anshtechsolution.smart_inventory.sales.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    @Transactional
    public SaleResponse createSale(CreateSaleRequest request) {

        // Reject duplicate product IDs in a single sale.
        Set<Long> productIds = new HashSet<>();

        for (SaleItemRequest item : request.getItems()) {
            if (!productIds.add(item.getProductId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each product must appear only once per sale");
            }
        }

        // Lock products in a consistent order to reduce deadlock risk.
        List<SaleItemRequest> requestedItems =
                new ArrayList<>(request.getItems());

        requestedItems.sort(
                Comparator.comparing(SaleItemRequest::getProductId));

        Map<Long, Product> products = new HashMap<>();

        for (SaleItemRequest item : requestedItems) {
            Product product = productRepository
                    .findByIdForUpdate(item.getProductId())
                    .orElseThrow(() ->
                            new ProductNotFoundException(
                                    item.getProductId()));

            if (product.getQuantity() < item.getQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for product: "
                                + product.getName()
                                + ". Available: " + product.getQuantity());
            }

            products.put(product.getId(), product);
        }

        String saleNumber = "SALE-" + UUID.randomUUID();

        Sale sale = Sale.builder()
                .saleNumber(saleNumber)
                .customerName(request.getCustomerName())
                .totalAmount(BigDecimal.ZERO)
                .status("COMPLETED")
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<StockMovement> movements = new ArrayList<>();

        // Preserve the order supplied by the client in the response.
        for (SaleItemRequest item : request.getItems()) {
            Product product = products.get(item.getProductId());

            BigDecimal unitPrice = product.getUnitPrice();
            BigDecimal lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(item.getQuantity()));

            SaleItem saleItem = SaleItem.builder()
                    .sale(sale)
                    .product(product)
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build();

            sale.getItems().add(saleItem);
            total = total.add(lineTotal);

            product.setQuantity(
                    product.getQuantity() - item.getQuantity());

            movements.add(StockMovement.builder()
                    .product(product)
                    .movementType("OUT")
                    .quantity(item.getQuantity())
                    .reference(saleNumber)
                    .notes("Stock deducted for sale")
                    .build());
        }

        sale.setTotalAmount(total);

        Sale savedSale = saleRepository.save(sale);
        productRepository.saveAll(products.values());
        movementRepository.saveAll(movements);

        return toResponse(savedSale);
    }

    @Transactional(readOnly = true)
    public SaleResponse getSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sale not found: " + id));

        return toResponse(sale);
    }

    private SaleResponse toResponse(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(item -> SaleItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return SaleResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .customerName(sale.getCustomerName())
                .totalAmount(sale.getTotalAmount())
                .status(sale.getStatus())
                .createdAt(sale.getCreatedAt())
                .items(items)
                .build();
    }
}