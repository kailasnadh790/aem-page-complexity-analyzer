package com.adobe.complexityanalyzer.core.services;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageInfoProvider;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = PageInfoProvider.class,
    property = {
        "service.description=Export page complexity metadata related to a page",
        "pageInfoProviderType=sites.listView.info.provider.complexity"
    },
    immediate = true
)
public class nodeCountInfoProvider implements PageInfoProvider {

    private static final Logger LOG = LoggerFactory.getLogger(nodeCountInfoProvider.class);
    private static final String PROVIDER_TYPE = "complexity";

    @Override
    public void updatePageInfo(SlingHttpServletRequest request, JSONObject info, Resource resource) throws JSONException {
        if (resource == null) {
            LOG.warn("ComplexityInfoProvider: updatePageInfo called with null resource");
            return;
        }
        
        LOG.info("ComplexityInfoProvider: updatePageInfo called for resource path: {}", resource.getPath());
        
        Page page = resource.adaptTo(Page.class);
        JSONObject complexityInfo = new JSONObject();

        if (page != null) {
            LOG.info("ComplexityInfoProvider: Page adapted successfully for path: {}", page.getPath());
            Resource contentResource = page.getContentResource();
            
            if (contentResource != null) {
                LOG.info("ComplexityInfoProvider: Content resource found at path: {}", contentResource.getPath());
                ValueMap properties = contentResource.getValueMap();
                
                // Get the complexity property
                String complexity = properties.get("complexity", String.class);
                
                if (complexity != null) {
                    LOG.info("ComplexityInfoProvider: Found complexity property, value: {}", complexity);
                    
                    complexityInfo.put("complexity", complexity);
                    LOG.info("ComplexityInfoProvider: Successfully added complexity='{}' to nested info for page: {}", 
                            complexity, page.getPath());
                } else {
                    LOG.warn("ComplexityInfoProvider: complexity property is NULL for page: {}. Available properties: {}", 
                            page.getPath(), properties.keySet());
                }
            } else {
                LOG.warn("ComplexityInfoProvider: Content resource (jcr:content) not found for page: {}", page.getPath());
            }
        } else {
            LOG.warn("ComplexityInfoProvider: Resource could not be adapted to Page: {}", resource.getPath());
        }
        
        // Add the nested object under the provider type
        info.put(PROVIDER_TYPE, complexityInfo);
        LOG.info("ComplexityInfoProvider: Added provider data to info object under key '{}'", PROVIDER_TYPE);
    }
}
