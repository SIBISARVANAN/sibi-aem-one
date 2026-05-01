package com.sibi.aem.one.core.models;

import com.sibi.aem.one.core.configs.HeaderConfig;
import com.sibi.aem.one.core.configs.HeaderNavItemsConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.osgi.resource.Resource;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Model(adaptables = {SlingHttpServletRequest.class, Resource.class})
public class HeaderModel {

    @Self
    private SlingHttpServletRequest request;

    private String logoPath;

    private boolean enableSiteSearch;

    private List<HeaderNavItemsConfig> navItems;

    @PostConstruct
    protected void init(){
        ConfigurationBuilder configBuilder = request.adaptTo(ConfigurationBuilder.class);
        if(configBuilder != null){
            HeaderConfig headerConfig = configBuilder.as(HeaderConfig.class);
            if(headerConfig != null){
                logoPath = headerConfig.logoPath();
                enableSiteSearch = headerConfig.enableSiteSearch();
            }
            navItems = new ArrayList<>(configBuilder.asCollection(HeaderNavItemsConfig.class));
        }
    }

    public String getLogoPath(){
        return logoPath;
    }

    public boolean getEnableSiteSearch(){
        return enableSiteSearch;
    }

    public List<HeaderNavItemsConfig> getNavItems(){
        return Collections.unmodifiableList(navItems);
    }
}
