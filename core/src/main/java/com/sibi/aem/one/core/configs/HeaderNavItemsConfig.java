package com.sibi.aem.one.core.configs;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Header Navigation Menu Items Configuration", description = "Configure navigation links for different sites", collection = true)
public @interface HeaderNavItemsConfig {

    @Property(label = "Page Name")
    String pageName();

    @Property(label = "Page Path")
    String pagePath();
}
