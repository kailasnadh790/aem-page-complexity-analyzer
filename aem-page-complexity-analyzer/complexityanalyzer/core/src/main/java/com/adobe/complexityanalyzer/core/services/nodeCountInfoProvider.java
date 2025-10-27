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
        "service.description=Export node count metadata related to a page",
        "pageInfoProviderType=sites.listView.info.provider.nodecount"
    },
    immediate = true
)
public class nodeCountInfoProvider implements PageInfoProvider {

    private static final Logger LOG = LoggerFactory.getLogger(nodeCountInfoProvider.class);
    private static final String PROVIDER_TYPE = "nodecount";

    @Override
    public void updatePageInfo(SlingHttpServletRequest request, JSONObject info, Resource resource) throws JSONException {
        if (resource == null) {
            LOG.warn("NodeCountInfoProvider: updatePageInfo called with null resource");
            return;
        }
        
        LOG.info("NodeCountInfoProvider: updatePageInfo called for resource path: {}", resource.getPath());
        
        Page page = resource.adaptTo(Page.class);
        JSONObject nodecountInfo = new JSONObject();

        if (page != null) {
            LOG.info("NodeCountInfoProvider: Page adapted successfully for path: {}", page.getPath());
            Resource contentResource = page.getContentResource();
            
            if (contentResource != null) {
                LOG.info("NodeCountInfoProvider: Content resource found at path: {}", contentResource.getPath());
                ValueMap properties = contentResource.getValueMap();
                
                // Try to get the property - it might be stored as different types
                Object nodeCountObj = properties.get("nodeCount");
                
                if (nodeCountObj != null) {
                    LOG.info("NodeCountInfoProvider: Found nodeCount property, type: {}, value: {}", 
                            nodeCountObj.getClass().getName(), nodeCountObj);
                    
                    String nodeCount = String.valueOf(nodeCountObj);
                    nodecountInfo.put("nodeCount", nodeCount);
                    LOG.info("NodeCountInfoProvider: Successfully added nodeCount='{}' to nested info for page: {}", 
                            nodeCount, page.getPath());
                } else {
                    LOG.warn("NodeCountInfoProvider: nodeCount property is NULL for page: {}. Available properties: {}", 
                            page.getPath(), properties.keySet());
                }
            } else {
                LOG.warn("NodeCountInfoProvider: Content resource (jcr:content) not found for page: {}", page.getPath());
            }
        } else {
            LOG.warn("NodeCountInfoProvider: Resource could not be adapted to Page: {}", resource.getPath());
        }
        
        // Add the nested object under the provider type
        info.put(PROVIDER_TYPE, nodecountInfo);
        LOG.info("NodeCountInfoProvider: Added provider data to info object under key '{}'", PROVIDER_TYPE);
    }
}
