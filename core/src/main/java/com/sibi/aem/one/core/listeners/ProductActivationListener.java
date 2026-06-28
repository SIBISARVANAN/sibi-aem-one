package com.sibi.aem.one.core.listeners;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Listens for changes to product pages and AUTOMATICALLY RE-PUBLISHES them
 * using the programmatic {@link Replicator} API.
 *
 * <h2>Real-World Scenario</h2>
 * <p>When an editor updates a product's price or stock-related field on an
 * already-published product page, the business wants the change to go live
 * immediately without the editor manually clicking "Publish" — e.g. price
 * corrections must propagate instantly to avoid showing the wrong price.</p>
 *
 * <h2>Technical Intricacies Demonstrated</h2>
 * <ul>
 *   <li><strong>Programmatic replication trigger</strong> — calling
 *       {@code Replicator.replicate()} from Java instead of relying on a human
 *       clicking the Publish button.</li>
 *   <li><strong>Async replication</strong> — using {@link ReplicationOptions} to
 *       avoid blocking the resource-change-event thread.</li>
 *   <li><strong>Service-user JCR session</strong> — replication requires a
 *       {@code Session}, obtained from a service-user {@code ResourceResolver}.</li>
 *   <li><strong>Self-trigger-loop avoidance</strong> — replication itself causes a
 *       JCR write under {@code jcr:content/cq:lastReplicat*}, which could re-fire
 *       this listener. We exclude replication-status properties from re-triggering.</li>
 * </ul>
 */
@Component(service = ResourceChangeListener.class, immediate = true, property = {
        ResourceChangeListener.PATHS   + "=glob:/content/sibi-aem-one/**/products/**/jcr:content",
        ResourceChangeListener.CHANGES + "=CHANGED"
})
public class ProductActivationListener implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ProductActivationListener.class);

    /** Service user subservice name — must be mapped in ServiceUserMapperImpl.amended config. */
    private static final String SERVICE_USER = "product-replication-service";

    @Reference
    private Replicator replicator;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void onChange(List<ResourceChange> changes) {
        for (ResourceChange change : changes) {
            String path = change.getPath();

            // Guard against re-trigger loops caused by replication's own JCR write
            // (cq:lastReplicationAction etc. are written to the SAME jcr:content node).
            if (change.isExternal()) {
                // External cluster changes are not re-published from this node —
                // the node that made the original change already triggers it.
                continue;
            }

            String pagePath = path.replace("/jcr:content", "");
            LOG.debug("Detected change on product page: {}", pagePath);
            triggerReplication(pagePath);
        }
    }

    /**
     * Triggers asynchronous activation (publish) of the given page path using
     * the programmatic {@link Replicator} API.
     *
     * <h3>Why async?</h3>
     * <p>{@code ReplicationOptions.setSynchronous(false)} ensures this call returns
     * immediately, queuing the replication action. The event-change thread must
     * never block on a network call to the publish instance — that would back up
     * the entire Sling observation event queue for this AEM node.</p>
     */
    private void triggerReplication(String pagePath) {
        ResourceResolver resolver = null;
        try {
            resolver = getServiceResolver();
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                LOG.error("Could not adapt service ResourceResolver to JCR Session for {}", pagePath);
                return;
            }

            ReplicationOptions options = new ReplicationOptions();
            options.setSynchronous(false);          // never block the event thread
            options.setSuppressVersions(true);       // don't create a JCR version per auto-publish

            replicator.replicate(session, ReplicationActionType.ACTIVATE, pagePath, options);
            LOG.info("Queued auto-publish (ACTIVATE) for product page: {}", pagePath);

        } catch (LoginException e) {
            LOG.error("Service user login failed for replication of {}: {}", pagePath, e.getMessage(), e);
        } catch (ReplicationException e) {
            // Common cause: service user lacks crx:replicate permission on this path.
            LOG.error("Replication failed for {}: {}", pagePath, e.getMessage(), e);
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close(); // ALWAYS close service resolvers — prevents session leak
            }
        }
    }

    private ResourceResolver getServiceResolver() throws LoginException {
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
        return resourceResolverFactory.getServiceResourceResolver(authInfo);
    }
}
