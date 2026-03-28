package com.sibi.aem.one.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Author Data Update scheduler", description = "Runs in scheduled intervals to update Author data")
public @interface AuthorUpdateSchedulerConfig {

    @AttributeDefinition(name = "Enabled", description = "Enable or Disable this scheduler", type = AttributeType.BOOLEAN)
    boolean enabled() default false;

    @AttributeDefinition(name = "CRON Expression", description = "CRON expression for scheduling this", type = AttributeType.STRING)
    String cronExpression() default "";

}
