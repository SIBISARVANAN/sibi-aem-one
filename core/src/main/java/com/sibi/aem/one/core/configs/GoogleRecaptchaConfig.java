package com.sibi.aem.one.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Google Recaptcha Factory Configuration", description = "Add multiple configurations for different sites")
public @interface GoogleRecaptchaConfig {

    @AttributeDefinition(name = "Site name", description = "Enter the website name for this particular configuration entry", type = AttributeType.STRING, required = true)
    public String siteName();

    @AttributeDefinition(name = "Public key", description = "Enter the Google recaptcha PUBLIC KEY for this site", type = AttributeType.STRING, required = true)
    public String publicKey();

    @AttributeDefinition(name = "Private key", description = "Enter the Google recaptcha PRIVATE KEY for this site", type = AttributeType.STRING, required = true)
    public String privateKey();

}
