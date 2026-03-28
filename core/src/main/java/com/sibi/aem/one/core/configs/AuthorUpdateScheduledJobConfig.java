package com.sibi.aem.one.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Author Update Scheduled Job Configuration")
public @interface AuthorUpdateScheduledJobConfig {

    @AttributeDefinition(name = "Enabled", description = "Enable or disable this scheduled job")
    boolean isEnabled() default false;

    @AttributeDefinition(name = "Config Param", description = "A custom parameter passed to the job")
    String configParam() default "default";

    @AttributeDefinition(name = "Cron Expression", description = "Quartz cron expression to schedule the job")
    String cronExpression() default "0 0 2 * * ?"; // every day at 2 AM

}
