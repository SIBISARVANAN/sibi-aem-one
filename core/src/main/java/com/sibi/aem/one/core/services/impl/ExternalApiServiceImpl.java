package com.sibi.aem.one.core.services.impl;

import com.sibi.aem.one.core.configs.ApiConfig;
import com.sibi.aem.one.core.services.ExternalApiService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component(service = ExternalApiService.class, immediate = true)
@Designate(ocd = ApiConfig.class)
public class ExternalApiServiceImpl implements ExternalApiService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalApiServiceImpl.class.getName());

    private CloseableHttpClient httpClient;
    private String apiBaseUrl;

    @Activate
    protected void activate(ApiConfig apiConfig) {
        this.apiBaseUrl = apiConfig.endpoint_url();
        // 1. Initialize the Connection Manager for pooling
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(apiConfig.max_connections());
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(20);

        // 2. Set timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(apiConfig.connection_timeout()) // Time to establish TCP handshake
                .setSocketTimeout(apiConfig.connection_timeout())  // Time to wait for data packets
                .setConnectionRequestTimeout(2000)                 // THE WAIT LIMIT for new connection when all existing connections in the pool are Leased
                .build();

        // 3. Create the client (this persists for the lifecycle of the service)
        this.httpClient = HttpClients.custom().setConnectionManager(poolingHttpClientConnectionManager).setDefaultRequestConfig(requestConfig).build();

        LOG.info("ExternalApiService Activated with endpoint: {}", apiBaseUrl);
    }

    @Override
    public String fetchProductData(String sku) {
        HttpGet httpGet = new HttpGet(apiBaseUrl + "/" + sku);
        // Under the hood, this method calls manager.requestConnection()
        try(CloseableHttpResponse response = httpClient.execute(httpGet)){
            int status = response.getStatusLine().getStatusCode();
            if(status == 200){
                return EntityUtils.toString(response.getEntity());
            } else {
                LOG.error("API returned error status: {}", status);
            }
            // When this block ends, the connection is "released" back to the pool
        } catch (IOException e) {
            LOG.error("Network error while fetching SKU: {}", sku, e);
        }
        return null;
    }

    @Deactivate
    protected void deactivate(){
        try{
            if(httpClient != null){
                httpClient.close();
            }
        } catch (IOException e) {
            LOG.error("Error closing HttpClient", e);
        }
    }
}
