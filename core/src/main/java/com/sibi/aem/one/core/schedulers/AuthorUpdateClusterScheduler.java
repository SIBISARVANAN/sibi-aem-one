package com.sibi.aem.one.core.schedulers;

import com.sibi.aem.one.core.configs.AuthorUpdateClusterSchedulerConfig;
import com.sibi.aem.one.core.configs.AuthorUpdateSchedulerConfig;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component(service = Runnable.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = AuthorUpdateClusterSchedulerConfig.class)
public class AuthorUpdateClusterScheduler implements Runnable {

    // this scheduler is cluster safe.
    // when there are multiple aem instances in a cluster sharing a single repository - MongoMK, this is useful.
    // when there are multiple crx repositories or when TarMK is used, this is not useful.
    private static final Logger LOG = LoggerFactory.getLogger(AuthorUpdateClusterScheduler.class);

    private boolean enabled;

    private String schedulerName;

    @Reference
    private Scheduler scheduler;

    private final ReentrantLock jvmLock = new ReentrantLock();

    private static final String LOCK_PATH = "/var/sibi-aem-one/locks/author/update";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    protected void activate(AuthorUpdateClusterSchedulerConfig config) {
        this.enabled = config.enabled();
        schedulerName = "sibi-aem-one.author.cluster.update";
        if (enabled) {
            ScheduleOptions scheduleOptions = scheduler.EXPR(config.cronExpression());
            scheduleOptions.name(schedulerName);
            scheduleOptions.canRunConcurrently(false);
            scheduler.schedule(this, scheduleOptions);
            LOG.debug(schedulerName + " scheduled");
        }

    }

    @Deactivate
    protected void deactivate() {
        scheduler.unschedule(schedulerName);
        LOG.debug(schedulerName + " unscheduled");
    }

    @Override
    public void run() {
        if(!jvmLock.tryLock()) {
            LOG.debug("Previous scheduled job is still running. Skipping this execution");
            return;
        }
        try(ResourceResolver resolver = getResourceResolver()){
            if(!getClusterLock(resolver)){
                LOG.debug("Another cluster job is running. Skipping this execution");
                return;
            }
            try{
                LOG.debug(schedulerName + " started");
            } finally {
                releaseClusterLock(resolver);
            }
        } catch (Exception e) {
            LOG.error("Scheduler run failed with exception ", e);
        } finally {
            jvmLock.unlock();
        }
    }

    private boolean getClusterLock (ResourceResolver resolver) throws RepositoryException {
        Session session = resolver.adaptTo(Session.class);
        LockManager lockManager = session.getWorkspace().getLockManager();

        if(!session.nodeExists(LOCK_PATH)){
            Node node = session.getRootNode();
            for (String path : LOCK_PATH.substring(1).split("/")) {
                node = node.hasNode(path) ? node.getNode(path) : node.addNode(path);
            }
            session.save();
        }
        if(lockManager.isLocked(LOCK_PATH)){
            return false;
        }
        lockManager.lock(LOCK_PATH, false, true, 300, null);
        return true;
    }

    private void releaseClusterLock (ResourceResolver resolver) throws RepositoryException {
        Session session = resolver.adaptTo(Session.class);
        LockManager lockManager = session.getWorkspace().getLockManager();
        if(lockManager.isLocked(LOCK_PATH)){
            lockManager.unlock(LOCK_PATH);
        }
    }

    private ResourceResolver getResourceResolver () throws LoginException {
        Map<String, Object> params = new HashMap<>();
        params.put(ResourceResolverFactory.SUBSERVICE, "scheduler");
        return resourceResolverFactory.getServiceResourceResolver(params);
    }
}
