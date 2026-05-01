package com.sibi.aem.one.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "External API Service Configuration")
public @interface ApiConfig {
    @AttributeDefinition(name = "API Endpoint URL", description = "Base URL for the product API")
    String endpoint_url() default "https://api.external.com/products/";

    @AttributeDefinition(name = "Connection Timeout", description = "Timeout in milliseconds")
    int connection_timeout() default 5000;

    @AttributeDefinition(name = "Max Connections", description = "Max total connections in the pool")
    int max_connections() default 50;
}
