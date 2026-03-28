package com.sibi.aem.one.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Multi-Timezone Scheduled Job Config")
public @interface MultiTimezoneJobConfig {

    @AttributeDefinition(name = "Enabled")
    boolean enabled() default true;

    // --- Timezone 1 ---
    @AttributeDefinition(name = "Timezone 1 ID", description = "e.g. America/New_York")
    String timezone1_id() default "America/New_York";

    @AttributeDefinition(name = "Timezone 1 Cron (UTC)", description = "Cron in UTC that maps to midnight in TZ1")
    String timezone1_cron() default "0 0 5 * * ?"; // UTC+0 when EST = UTC-5

    // --- Timezone 2 ---
    @AttributeDefinition(name = "Timezone 2 ID", description = "e.g. Europe/London")
    String timezone2_id() default "Europe/London";

    @AttributeDefinition(name = "Timezone 2 Cron (UTC)")
    String timezone2_cron() default "0 0 0 * * ?"; // UTC+0, midnight London

    // --- Timezone 3 ---
    @AttributeDefinition(name = "Timezone 3 ID", description = "e.g. Asia/Kolkata")
    String timezone3_id() default "Asia/Kolkata";

    @AttributeDefinition(name = "Timezone 3 Cron (UTC)")
    String timezone3_cron() default "0 30 18 * * ?"; // UTC+5:30, so 18:30 UTC = midnight IST
}