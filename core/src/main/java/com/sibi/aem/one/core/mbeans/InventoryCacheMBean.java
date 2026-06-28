package com.sibi.aem.one.core.mbeans;

import com.adobe.granite.jmx.annotation.Description;

@Description("Inventory Service Cache Management Control Panel")
public interface InventoryCacheMBean {

    @Description("Returns the current number of SKU items held in the stock cache.")
    int getCacheSize();

    @Description("DANGER: Instantly flushes all cached stock data. The next user requests will fetch fresh data from the API.")
    void clearCache();
}