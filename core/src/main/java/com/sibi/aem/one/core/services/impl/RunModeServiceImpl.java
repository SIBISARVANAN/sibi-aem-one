package com.sibi.aem.one.core.services.impl;

import com.sibi.aem.one.core.services.RunModeService;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Set;

@Component(service = RunModeService.class, immediate = true)
public class RunModeServiceImpl implements RunModeService {

    @Reference
    private SlingSettingsService slingSettingsService;

    @Override
    public boolean isAuthor() {
        return slingSettingsService.getRunModes().contains("author");
    }

    @Override
    public boolean isPublish() {
        return slingSettingsService.getRunModes().contains("publish");
    }

    @Override
    public boolean isDev() {
        return slingSettingsService.getRunModes().contains("dev");
    }

    @Override
    public boolean isQa() {
        return slingSettingsService.getRunModes().contains("qa");
    }

    @Override
    public boolean isStage() {
        return slingSettingsService.getRunModes().contains("stage");
    }

    @Override
    public boolean isProd() {
        return slingSettingsService.getRunModes().contains("prod");
    }

    @Override
    public Set<String> getAllRunModes() {
        return slingSettingsService.getRunModes();
    }
}
