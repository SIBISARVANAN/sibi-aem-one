package com.sibi.aem.one.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Author Data Update cluster scheduler", description = "Runs in scheduled intervals to update Author data in a clustered environment")
public @interface AuthorUpdateClusterSchedulerConfig {

    @AttributeDefinition(name = "Enabled", description = "Enable or Disable this scheduler", type = AttributeType.BOOLEAN)
    boolean enabled() default false;

    @AttributeDefinition(name = "CRON Expression", description = "CRON expression for scheduling this", type = AttributeType.STRING)
    String cronExpression() default "";

}

/*
 How the Wiring Works End to End


@ObjectClassDefinition       →  Tells OSGi: "this @interface describes a config"
@AttributeDefinition         →  Metadata for each field (name, description, type)
@Designate(ocd = Config.class) →  Links the @Component to this config schema
@Activate(Config config)     →  OSGi injects the live config values into your component

"@interface in OSGi is a Java annotation type repurposed as a configuration schema. Combined with @ObjectClassDefinition and @Designate, it lets OSGi automatically map .cfg.json properties into a typed config object injected into your component — with default values, metatype UI generation, and zero boilerplate."
 */