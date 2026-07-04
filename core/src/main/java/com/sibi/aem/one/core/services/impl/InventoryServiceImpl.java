package com.sibi.aem.one.core.services.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sibi.aem.one.core.mbeans.InventoryCacheMBean;
import com.sibi.aem.one.core.services.InventoryService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OSGi service that fetches live stock counts from an external inventory API.
 *
 * <h2>Patterns Demonstrated</h2>
 * <ul>
 *   <li>Pooled HTTP client created once in {@code @Activate}, closed in {@code @Deactivate}</li>
 *   <li>Simple in-memory cache (use Guava/Caffeine with TTL in production),
 *       cleared on {@code @Modified} so a config change invalidates stale entries</li>
 *   <li>Simple in-memory cache (use Guava/Caffeine with TTL in production),
 *       cleared on {@code @Modified} so a config change invalidates stale entries</li>
 *   <li>OSGi metatype config with {@code ConfigurationPolicy.REQUIRE} — the service
 *       will not start without an explicit config, preventing silent default-value runs</li>
 * </ul>
 */
@Component(service = {InventoryService.class, InventoryCacheMBean.class}, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, property = {
        "jmx.objectname=com.sibi.aem.one:type=CacheManagement,name=Inventory Stock Cache"
})
@Designate(ocd = InventoryServiceImpl.Config.class)
public class InventoryServiceImpl implements InventoryService, InventoryCacheMBean {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryServiceImpl.class);

    @ObjectClassDefinition(name = "Inventory Service Configuration")
    public @interface Config {
        @AttributeDefinition(name = "API Endpoint URL")
        String api_endpoint_url() default "https://inventory.internal/api/v1/stock/";

        @AttributeDefinition(name = "Connection Timeout (ms)")
        int connection_timeout() default 3000;

        @AttributeDefinition(name = "Max Connections")
        int max_connections() default 20;

        @AttributeDefinition(name = "Cache Enabled")
        boolean cache_enabled() default true;
    }

    private String apiEndpointUrl;
    private boolean cacheEnabled;
    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    /** Thread-safe — getStockCount() is called concurrently from multiple render threads. */
    private final Map<String, Integer> stockCache = new ConcurrentHashMap<>();

    /**
     * Guava Cache — bounded, TTL-evicting, thread-safe.
     *
     * Why NOT ConcurrentHashMap: it has NO eviction mechanism. With thousands of SKUs
     * in production it grows indefinitely in the JVM Old Generation for the entire AEM
     * instance lifetime — a classic heap leak visible in Eclipse MAT as a large retained
     * ConcurrentHashMap inside this singleton OSGi service.
     *
     * Why Guava Cache:
     *   maximumSize(500)  — evicts Least Recently Used once cap is hit; heap is bounded.
     *   expireAfterWrite  — entries evict after 5 min so stock counts don't go stale.
     *   Thread-safe       — no external synchronisation needed (same as ConcurrentHashMap).
     *
     * Guava is bundled in AEM — no new Maven dependency. In high-throughput environments,
     * prefer Caffeine (better async eviction) but it needs an explicit dependency.
     */
    private Cache<String, Integer> optimalStockCache;

    @Activate
    protected void activate(Config config) {
        this.apiEndpointUrl = config.api_endpoint_url();
        this.cacheEnabled   = config.cache_enabled();

        // Build (or rebuild) the bounded cache each time activate/modified runs.
        // Re-creating the cache object here also implicitly discards all stale entries
        // from any previous activation without needing an explicit .invalidateAll() call.
        optimalStockCache = CacheBuilder.newBuilder()
                .maximumSize(500)                        // LRU eviction above this count
                .expireAfterWrite(5, TimeUnit.MINUTES)   // TTL — stock counts refresh every 5 min
                .recordStats()                           // enables hit/miss stats via stockCache.stats()
                .build();

        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(config.max_connections());
        connectionManager.setDefaultMaxPerRoute(10);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.connection_timeout())
                .setSocketTimeout(config.connection_timeout())
                .setConnectionRequestTimeout(1500)
                .build();

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        LOG.info("InventoryService activated. Endpoint: {}, cache: {}", apiEndpointUrl, cacheEnabled);
    }

    /**
     * Clears the cache and reinitialize the client on config change. Without this,
     * @Activate alone would re-run on @Modified but leak the old HTTP client/pool.
     */
    @Modified
    protected void modified(Config config) {
        stockCache.clear();
        optimalStockCache.invalidateAll();
        deactivate();
        activate(config);
    }

    /**
     * Releases all resources on bundle stop / component deregistration:
     * closes pooled HTTP connections AND clears the in-memory cache.
     * Clearing the cache here was missing in the original version — leaving
     * stale entries in memory after deactivation serves no purpose and should
     * always be cleaned up alongside other resources.
     */
    @Deactivate
    protected void deactivate() {
        stockCache.clear();
        try {
            if (httpClient != null) httpClient.close();
            if (connectionManager != null) connectionManager.close();
        } catch (Exception e) {
            LOG.warn("Error closing HTTP client during deactivation: {}", e.getMessage());
        } finally {
            optimalStockCache.invalidateAll();
        }
    }

    @Override
    public int getStockCount(String sku) throws InventoryServiceException {
        if (StringUtils.isBlank(sku)) return -1;
        if (cacheEnabled && stockCache.containsKey(sku)) {
            return stockCache.get(sku);
        }
        if(cacheEnabled){
            // getIfPresent() returns null on a cache miss — never throws.
            // ConcurrentHashMap used containsKey() + get() which is two operations
            // and not atomic. Guava's getIfPresent() is a single atomic lookup.
            Integer cached = optimalStockCache.getIfPresent(sku);
            if (cached != null) {
                return cached;
            }
        }
        int count = fetchFromApi(sku);
        if (cacheEnabled && count >= 0) {
            stockCache.put(sku, count);
            optimalStockCache.put(sku, count);
        }
        return count;
    }

    private int fetchFromApi(String sku) throws InventoryServiceException {
        String url = apiEndpointUrl + sku;
        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");

            if (statusCode == 200) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                return json.has("stockCount") ? json.get("stockCount").getAsInt() : -1;
            } else {
                throw new InventoryServiceException(
                        "Inventory API error " + statusCode + " for sku " + sku, null);
            }
        } catch (InventoryServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new InventoryServiceException("Failed to call inventory API for sku " + sku, e);
        }
    }

    @Override
    public int getCacheSize() {
        return stockCache.size();
    }

    @Override
    public void clearCache() {
        stockCache.clear();
        optimalStockCache.invalidateAll();
        LOG.info("Inventory Stock Cache was instantly cleared via JMX MBean intervention.");
    }
}
