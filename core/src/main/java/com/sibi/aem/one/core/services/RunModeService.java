package com.sibi.aem.one.core.services;

import java.util.Set;

public interface RunModeService {

    public boolean isAuthor();

    public boolean isPublish();

    public boolean isDev();

    public boolean isQa();

    public boolean isStage();

    public boolean isProd();

    public Set<String> getAllRunModes();
}
