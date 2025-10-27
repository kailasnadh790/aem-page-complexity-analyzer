package com.adobe.complexityanalyzer.core.schedulers;

import com.adobe.complexityanalyzer.core.config.PageNodeCountSchedulerConfig;
import org.apache.sling.api.resource.*;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component(service = Runnable.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = PageNodeCountSchedulerConfig.class)
public class PageNodeCountJob implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PageNodeCountJob.class);
    private static final String JOB_NAME = "PageNodeCountJob";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Scheduler scheduler;

    private String rootPath;
    private boolean enabled;
    private String schedulerExpression;

    @Activate
    @Modified
    protected void activate(PageNodeCountSchedulerConfig config) {
        this.rootPath = config.rootPath();
        this.enabled = config.enabled();
        this.schedulerExpression = config.scheduler_expression();
        
        LOG.info("Activating PageNodeCountJob - enabled: {}, rootPath: {}, expression: {}", 
                enabled, rootPath, schedulerExpression);
        
        // Remove existing scheduled job
        scheduler.unschedule(JOB_NAME);
        
        // Schedule with config expression if enabled
        if (enabled) {
            try {
                ScheduleOptions options = scheduler.EXPR(schedulerExpression);
                options.name(JOB_NAME);
                options.canRunConcurrently(false);
                scheduler.schedule(this, options);
                LOG.info("PageNodeCountJob successfully scheduled with cron expression: {}", schedulerExpression);
            } catch (Exception e) {
                LOG.error("Failed to schedule PageNodeCountJob with expression: {}", schedulerExpression, e);
            }
        } else {
            LOG.info("PageNodeCountJob is disabled via configuration");
        }
    }
    
    @Deactivate
    protected void deactivate() {
        LOG.info("Deactivating PageNodeCountJob");
        scheduler.unschedule(JOB_NAME);
        LOG.info("PageNodeCountJob unscheduled");
    }

    @Override
    public void run() {
        if (!enabled) {
            LOG.debug("PageNodeCountJob skipped - job is disabled");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        LOG.info("Starting PageNodeCountJob execution for root path: {}", rootPath);

        Map<String, Object> authInfo = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, "nodecount-updater");
        
        try (ResourceResolver rr = resolverFactory.getServiceResourceResolver(authInfo)) {
            
            Resource rootResource = rr.getResource(rootPath);
            if (rootResource == null) {
                LOG.warn("Root path not found: {}. Job execution aborted.", rootPath);
                return;
            }
            
            LOG.debug("Root resource found: {}. Starting page traversal...", rootPath);
            AtomicInteger pagesProcessed = new AtomicInteger(0);
            walkAndCount(rr, rootResource, pagesProcessed);
            
            long duration = System.currentTimeMillis() - startTime;
            LOG.info("PageNodeCountJob completed successfully. Pages processed: {}, Duration: {} ms", 
                    pagesProcessed.get(), duration);
            
        } catch (LoginException e) {
            LOG.error("Failed to obtain ResourceResolver. Check service user configuration.", e);
        } catch (Exception e) {
            LOG.error("Unexpected error running PageNodeCountJob for path: {}", rootPath, e);
        }
    }

    private void walkAndCount(ResourceResolver rr, Resource root, AtomicInteger pagesProcessed) {
        if (root == null) {
            LOG.debug("Null resource encountered, skipping");
            return;
        }

        Iterator<Resource> children = root.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            String resourceType = child.getResourceType();
            String primaryType = child.getValueMap().get("jcr:primaryType", String.class);
            
            if ("cq:Page".equals(resourceType) || "cq:Page".equals(primaryType)) {
                String pagePath = child.getPath();
                LOG.debug("Processing page: {}", pagePath);
                
                Resource jcrContent = child.getChild("jcr:content");
                if (jcrContent != null) {
                    int count = countAllDescendants(jcrContent);
                    LOG.debug("Total descendants count for {}: {}", pagePath, count);
                    
                    try {
                        ModifiableValueMap props = jcrContent.adaptTo(ModifiableValueMap.class);
                        if (props != null) {
                            String oldValue = props.get("nodeCount", String.class);
                            String newValue = String.valueOf(count);
                            
                            // Always overwrite the property, even if it exists
                            props.put("nodeCount", newValue);
                            rr.commit();
                            pagesProcessed.incrementAndGet();
                            
                            if (oldValue != null) {
                                LOG.debug("Overwritten nodeCount for page: {} (old={}, new={})", pagePath, oldValue, newValue);
                            } else {
                                LOG.debug("Set nodeCount={} for page: {}", count, pagePath);
                            }
                        } else {
                            LOG.warn("Could not adapt jcr:content to ModifiableValueMap for: {}", pagePath);
                        }
                    } catch (PersistenceException e) {
                        LOG.warn("Failed to persist nodeCount for page: {}", pagePath, e);
                    }
                } else {
                    LOG.debug("No jcr:content found for page: {}", pagePath);
                }
            }
            // Recursively descend into this node
            walkAndCount(rr, child, pagesProcessed);
        }
    }

    private int countAllDescendants(Resource resource) {
        if (resource == null) {
            return 0;
        }
        int count = 0;
        for (Resource child : resource.getChildren()) {
            // Skip counting if this is a cq:Page (don't count nested pages)
            String primaryType = child.getValueMap().get("jcr:primaryType", String.class);
            if ("cq:Page".equals(primaryType)) {
                continue; // Don't count child pages or their descendants
            }
            
            count++; // Count this child
            count += countAllDescendants(child); // Count all its descendants recursively
        }
        return count;
    }
}
