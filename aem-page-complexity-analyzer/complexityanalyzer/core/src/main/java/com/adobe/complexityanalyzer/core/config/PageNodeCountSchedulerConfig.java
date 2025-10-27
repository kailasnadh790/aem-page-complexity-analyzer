package com.adobe.complexityanalyzer.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Page NodeCount Scheduler Config")
public @interface PageNodeCountSchedulerConfig {
    @AttributeDefinition(
        name = "Enabled",
        description = "Enable or disable the node count job"
    )
    boolean enabled() default true;

    @AttributeDefinition(
        name = "Content Root Path",
        description = "The root path under which pages will be checked (e.g. /content/mysite)"
    )
    String rootPath() default "/content/site";

    @AttributeDefinition(
        name = "Scheduler Cron Expression",
        description = "CRON expression to run the job (e.g. '0 0 * * * ?' for every hour)"
    )
    String scheduler_expression() default "0 0 * * * ?";
}
