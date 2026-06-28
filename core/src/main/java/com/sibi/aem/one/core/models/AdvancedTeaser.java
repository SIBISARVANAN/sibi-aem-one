package com.sibi.aem.one.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AdvancedTeaser {

    @ValueMapValue private String title;
    @ValueMapValue private String description;
    @ValueMapValue private String richDescription;
    @ValueMapValue private String teaserStyle;
    @ValueMapValue private String overlayColor;
    @ValueMapValue private String campaignCategory;
    @ValueMapValue private String secondaryCampaign;
    @ValueMapValue private String fileReference;
    @ValueMapValue private boolean enableTimer;
    @ValueMapValue private Date targetDate;

    @ChildResource(name = "buttons")
    private List<CtaButton> buttons;

    @ChildResource(name = "regions")
    private List<Region> regions;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRichDescription() { return richDescription; }
    public String getTeaserStyle() { return teaserStyle; }
    public String getOverlayColor() { return overlayColor; }
    public String getCampaignCategory() { return campaignCategory; }
    public String getSecondaryCampaign() { return secondaryCampaign; }
    public String getFileReference() { return fileReference; }
    public boolean isEnableTimer() { return enableTimer; }
    public Date getTargetDate() { return targetDate; }

    public List<CtaButton> getButtons() {
        return buttons != null ? buttons : Collections.emptyList();
    }

    public List<Region> getRegions() {
        return regions != null ? regions : Collections.emptyList();
    }
}