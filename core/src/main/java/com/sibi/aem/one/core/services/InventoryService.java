package com.sibi.aem.one.core.services;

/**
 * OSGi service interface for querying live inventory counts from an external
 * inventory microservice. Injected into
 * {@code com.sibi.aem.one.core.models.product.impl.ProductImpl} via
 * {@code @OSGiService} — demonstrating that Sling Models can consume any
 * registered OSGi service directly.
 */
public interface InventoryService {

    /**
     * @return available unit count, or -1 if the service is unavailable
     * @throws InventoryServiceException if the downstream API returns an error
     */
    int getStockCount(String sku) throws InventoryServiceException;

    class InventoryServiceException extends Exception {
        public InventoryServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
