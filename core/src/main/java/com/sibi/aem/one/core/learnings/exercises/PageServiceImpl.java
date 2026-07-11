package com.sibi.aem.one.core.learnings.exercises;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component(service = PageService.class, immediate = true)
public class PageServiceImpl implements PageService {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public String getTitle(String path) {
        try(ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(Collections.emptyMap())){
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            Page page = pageManager.getPage(path);
            return page.getTitle();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getThumbnail(String path) {
        try(ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(Collections.emptyMap())){
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            Page page = pageManager.getPage(path);
            String thumbnailPath = page.getContentResource().getValueMap().get("thumbnailPath", String.class);
            Resource assetResource = resourceResolver.getResource(thumbnailPath);
            Asset asset = assetResource.adaptTo(Asset.class);
            return asset.getPath();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getThumbnailTitle(String path) {
        try(ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(Collections.emptyMap())){
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            Page page = pageManager.getPage(path);
            String thumbnailPath = page.getContentResource().getValueMap().get("thumbnailPath", String.class);
            Resource assetResource = resourceResolver.getResource(thumbnailPath);
            Asset asset = assetResource.adaptTo(Asset.class);
            return asset.getMetadataValue("dc:title");
        } catch (Exception e) {
            return null;
        }
    }

    public String searchPageWithTitle(String title) {
        try(ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(Collections.emptyMap())){
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            Map<String, String> params = new HashMap<>();
            params.put("type", "nt:unstructured");
            params.put("path", "/content");
            params.put("property", "jcr:title");
            params.put("property.value", title);

            Session session = resourceResolver.adaptTo(Session.class);
            Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);
            SearchResult result = query.getResult();
            String pageTitle = "";
            for(Hit hit : result.getHits()){
                Resource resource = hit.getResource();
                pageTitle = pageManager.getContainingPage(resource).getTitle();
                if(StringUtils.equals(title, pageTitle)){
                    return resource.getPath();
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
}
