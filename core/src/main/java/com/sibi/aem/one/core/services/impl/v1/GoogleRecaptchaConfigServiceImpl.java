package com.sibi.aem.one.core.services.impl.v1;

import com.sibi.aem.one.core.configs.GoogleRecaptchaConfig;
import com.sibi.aem.one.core.services.GoogleRecaptchaConfigService;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = GoogleRecaptchaConfigService.class, name = "v1", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = GoogleRecaptchaConfig.class, factory = true)
public class GoogleRecaptchaConfigServiceImpl implements GoogleRecaptchaConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleRecaptchaConfigServiceImpl.class);

    private String siteName;

    private String publicKey;

    private String privateKey;

    @Activate
    @Modified
    public void activate(GoogleRecaptchaConfig googleRecaptchaConfig) {
        siteName = googleRecaptchaConfig.siteName();
        publicKey = googleRecaptchaConfig.publicKey();
        privateKey = googleRecaptchaConfig.privateKey();
    }

    @Override
    public String getSiteName() {
        return siteName;
    }

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public String getPrivateKey() {
        return privateKey;
    }

    @Component(service = Registry.class, immediate = true)
    public static class Registry {
        private static final Map<String, GoogleRecaptchaConfigService> REGISTRY = new ConcurrentHashMap<>();

        public static GoogleRecaptchaConfigService getGoogleRecaptchaConfigService(String siteName) {
            return REGISTRY.get(siteName);
        }

        public static Map<String, GoogleRecaptchaConfigService> getRegistry() {
            return Collections.unmodifiableMap(REGISTRY);
        }

        @Reference(service = GoogleRecaptchaConfigService.class,
                cardinality = ReferenceCardinality.MULTIPLE,
                policy = ReferencePolicy.DYNAMIC)
        protected void bind(GoogleRecaptchaConfigService googleRecaptchaConfigService) {
            REGISTRY.put(googleRecaptchaConfigService.getSiteName(), googleRecaptchaConfigService);
        }

        protected void unbind(GoogleRecaptchaConfigService googleRecaptchaConfigService) {
            REGISTRY.remove(googleRecaptchaConfigService.getSiteName());
        }
    }
}
