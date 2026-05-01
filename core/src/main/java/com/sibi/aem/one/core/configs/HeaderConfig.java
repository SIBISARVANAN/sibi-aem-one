package com.sibi.aem.one.core.configs;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Header Configuration", description = "Configuration of site header for different sites")
public @interface HeaderConfig {

    @Property(label = "Logo Path")
    String logoPath();

    @Property(label = "Enable Site Search")
    boolean enableSiteSearch() default true;

}
