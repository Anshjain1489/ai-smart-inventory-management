package com.anshtechsolution.smart_inventory.sales;

public interface SalesVelocityProjection {

    Long getProductId();

    String getProductName();

    Long getUnitsSold();
}